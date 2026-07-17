package com.vesoft.nebula.jdbc.sql.query;

import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaExpression;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaOrderByItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaTableSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NebulaQueryStatementAdapter {

	public NebulaQueryStatement adapt(NebulaSqlParseResult parseResult) throws SQLException {
		if (parseResult == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.INVALID_INPUT, "parse result must not be null");
		}
		NebulaSqlStatement statement = parseResult.getStatement();
		if (!(statement instanceof NebulaSelectStatement)) {
			return NebulaQueryStatement.nonQuery(toQueryKind(parseResult.getKind()));
		}
		return adaptSelect((NebulaSelectStatement) statement);
	}

	public NebulaQueryStatement adaptSelect(NebulaSelectStatement select) throws SQLException {
		if (select == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.INVALID_INPUT, "select statement must not be null");
		}
		if (select.getUnion() != null) {
			return unsupported(UnsupportedQueryFeature.UNION, "UNION is not graph-mappable by the query adapter");
		}
		if (!select.getGroupBy().isEmpty() || select.getHaving() != null || hasAggregate(select.getSelectItems())) {
			return unsupported(UnsupportedQueryFeature.FUNCTION, "Aggregate, GROUP BY and HAVING require explicit graph aggregation templates");
		}
		NebulaTableSource from = select.getFrom();
		if (from == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.MISSING_TABLE_MAPPING, "SELECT statement has no FROM table");
		}
		if (from.getType() == NebulaTableSource.Type.JOIN) {
			return unsupported(UnsupportedQueryFeature.JOIN, "JOIN requires an explicit tag-edge-tag mapping template");
		}
		if (from.getType() == NebulaTableSource.Type.SUBQUERY) {
			return unsupported(UnsupportedQueryFeature.SUBQUERY, "Subquery source requires an explicit graph query template");
		}

		NebulaQueryStatement.Builder builder = NebulaQueryStatement.select(from.getName())
				.alias(from.getAlias() == null ? from.getName() : from.getAlias());
			if (isSelectAll(select.getSelectItems())) {
				builder.selectAll();
			} else {
				for (NebulaSelectItem item : select.getSelectItems()) {
					builder.projection(adaptExpression(item.getExpression(), from), item.getAlias());
				}
			}
			if (select.getWhere() != null) {
				builder.where(adaptExpression(select.getWhere(), from));
		}
		if (!select.getOrderBy().isEmpty()) {
			if (select.getOrderBy().size() > 1) {
				throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
						"Only one ORDER BY item is supported");
			}
			NebulaOrderByItem orderBy = select.getOrderBy().get(0);
			builder.orderBy(columnName(orderBy.getExpression()), sortDirection(orderBy.getDirection()));
		}
		if (select.getLimit() != null) {
			if (select.getLimit().getRowCount() != null) {
				builder.limit(select.getLimit().getRowCount().intValue());
			}
			if (select.getLimit().getOffset() != null) {
				builder.offset(select.getLimit().getOffset().intValue());
			}
		}
		return builder.build();
	}

	private QueryExpression adaptExpression(NebulaExpression expression, NebulaTableSource from) throws SQLException {
		if (expression.getType() == NebulaExpression.Type.COLUMN) {
			return QueryExpression.column(from.getName(), unqualified(expression.getName()));
		}
		if (expression.getType() == NebulaExpression.Type.LITERAL) {
			return QueryExpression.literal(expression.getValue());
		}
		if (expression.getType() == NebulaExpression.Type.BINARY) {
			String operator = expression.getOperator();
			if ("=".equals(operator) || "==".equals(operator)) {
				return QueryExpression.eq(adaptExpression(expression.getLeft(), from), adaptExpression(expression.getRight(), from));
			}
			if (">=".equals(operator)) {
				return QueryExpression.gte(adaptExpression(expression.getLeft(), from), adaptExpression(expression.getRight(), from));
			}
			if ("AND".equalsIgnoreCase(operator) || "&&".equals(operator)) {
				return QueryExpression.and(adaptExpression(expression.getLeft(), from), adaptExpression(expression.getRight(), from));
			}
			}
			if (expression.getType() == NebulaExpression.Type.FUNCTION) {
				if (expression.isAggregate()) {
					throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
							"Aggregate expression requires explicit graph aggregation template: " + expression.getName());
				}
				return QueryExpression.function(expression.getName(), adaptArguments(expression.getArguments(), from));
			}
		if (expression.getType() == NebulaExpression.Type.PARAMETER) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
					"Parameter binding is not available in NebulaQueryStatement adapter yet");
		}
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
					"Unsupported expression type: " + expression.getType());
		}

		private List<QueryExpression> adaptArguments(List<NebulaExpression> arguments, NebulaTableSource from) throws SQLException {
			List<QueryExpression> adapted = new ArrayList<QueryExpression>();
			for (NebulaExpression argument : arguments) {
				adapted.add(adaptExpression(argument, from));
			}
			return adapted;
		}

	private StatementKind toQueryKind(com.vesoft.nebula.jdbc.sql.parser.StatementKind kind) {
		if (kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.INSERT) {
			return StatementKind.INSERT;
		}
		if (kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.UPDATE) {
			return StatementKind.UPDATE;
		}
		if (kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.DELETE) {
			return StatementKind.DELETE;
		}
		if (kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.CREATE_SCHEMA ||
				kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.DROP_SCHEMA ||
				kind == com.vesoft.nebula.jdbc.sql.parser.StatementKind.METADATA) {
			return StatementKind.DDL;
		}
		return StatementKind.SELECT;
	}

	private boolean hasAggregate(List<NebulaSelectItem> items) {
		for (NebulaSelectItem item : items) {
			if (item.getExpression() != null && item.getExpression().isAggregate()) {
				return true;
			}
		}
		return false;
	}

	private boolean isSelectAll(List<NebulaSelectItem> items) {
		return items.isEmpty() || (items.size() == 1 &&
				items.get(0).getExpression() != null &&
				items.get(0).getExpression().getType() == NebulaExpression.Type.WILDCARD);
	}

	private NebulaQueryStatement unsupported(UnsupportedQueryFeature feature, String reason) {
		return NebulaQueryStatement.select("__unsupported__")
				.unsupported(feature, reason)
				.build();
	}

	private String columnName(NebulaExpression expression) throws SQLException {
		if (expression == null || expression.getType() != NebulaExpression.Type.COLUMN) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
					"ORDER BY only supports mapped column references");
		}
		return unqualified(expression.getName());
	}

	private String unqualified(String name) {
		int dot = name == null ? -1 : name.lastIndexOf('.');
		return dot < 0 ? name : name.substring(dot + 1);
	}

	private SortDirection sortDirection(String direction) {
		return "DESC".equalsIgnoreCase(direction) ? SortDirection.DESC : SortDirection.ASC;
	}
}
