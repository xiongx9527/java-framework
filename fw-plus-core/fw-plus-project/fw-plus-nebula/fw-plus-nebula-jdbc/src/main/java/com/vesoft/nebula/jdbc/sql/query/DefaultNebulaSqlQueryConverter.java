package com.vesoft.nebula.jdbc.sql.query;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultNebulaSqlQueryConverter implements NebulaSqlQueryConverter {
	@Override
	public NgqlQueryPlan convert(NebulaQueryStatement statement,
								 NebulaQueryBindings bindings,
								 NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		if (statement == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.INVALID_INPUT, "statement must not be null");
		}
		if (statement.getKind() != StatementKind.SELECT) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.QUERY_ONLY, "SQL query converter only supports SELECT statements");
		}
		if (statement.getUnsupportedFeature() != null) {
			throw NebulaQueryConversionException.of(statement.getUnsupportedFeature().getErrorCode(), statement.getUnsupportedReason());
		}
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTableName());
		String alias = statement.getAlias();
		List<String> trace = new ArrayList<String>();
		StringBuilder ngql = new StringBuilder();
		if (tableMapping.getTableKind() == TableKind.TAG) {
			trace.add("table " + statement.getTableName() + " -> TAG " + tableMapping.getNebulaName());
			ngql.append("MATCH (").append(alias).append(":").append(tableMapping.getNebulaName()).append(")");
		} else if (tableMapping.getTableKind() == TableKind.EDGE) {
			trace.add("table " + statement.getTableName() + " -> EDGE " + tableMapping.getNebulaName());
			ngql.append("MATCH (src)-[").append(alias).append(":").append(tableMapping.getNebulaName()).append("]->(dst)");
		} else {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_TABLE_KIND,
					"Unsupported table mapping kind: " + tableMapping.getTableKind());
		}
		if (statement.getWhere() != null) {
			String filter = renderExpression(statement.getWhere(), tableMapping, alias, trace);
			ngql.append(" WHERE ").append(filter);
			trace.add("filter " + filter);
		}
		ngql.append(" RETURN ");

		List<QueryColumn> columns = new ArrayList<QueryColumn>();
		List<String> projections = new ArrayList<String>();
		if (!statement.getProjections().isEmpty() && !statement.isSelectAll()) {
			addExplicitProjections(statement, tableMapping, alias, trace, columns, projections);
		} else {
			addSelectAllProjections(tableMapping, alias, columns, projections);
		}
		for (int i = 0; i < projections.size(); i++) {
			if (i > 0) {
				ngql.append(", ");
			}
			ngql.append(projections.get(i));
		}
		trace.add("projection " + projections);
		if (statement.getOrderByColumn() != null) {
			ColumnMapping orderColumn = tableMapping.getRequiredColumn(statement.getOrderByColumn());
			ngql.append(" ORDER BY ").append(renderColumn(orderColumn, tableMapping, alias))
					.append(" ").append(statement.getSortDirection().name());
			trace.add("order " + statement.getOrderByColumn() + " " + statement.getSortDirection().name());
		}
		if (statement.getLimit() != null) {
			ngql.append(" LIMIT ");
			if (statement.getOffset() != null) {
				ngql.append(statement.getOffset()).append(", ");
			}
			ngql.append(statement.getLimit());
			trace.add("pagination offset=" + (statement.getOffset() == null ? 0 : statement.getOffset()) +
					" limit=" + statement.getLimit());
		}

		QueryKind queryKind = tableMapping.getTableKind() == TableKind.TAG ? QueryKind.VERTEX_QUERY : QueryKind.EDGE_QUERY;
		QueryFidelity fidelity = tableMapping.getTableKind() == TableKind.TAG ?
				QueryFidelity.EQUIVALENT : QueryFidelity.CONTROLLED_EQUIVALENT;
		return new NgqlQueryPlan(ngql.toString(), queryKind, columns, fidelity, trace);
	}

	private String renderExpression(QueryExpression expression,
									TableMapping tableMapping,
									String alias,
									List<String> trace) throws SQLException {
		if (expression.getType() == QueryExpression.Type.EQ) {
			return renderExpression(expression.getLeft(), tableMapping, alias, trace) + " == " +
					renderExpression(expression.getRight(), tableMapping, alias, trace);
		}
		if (expression.getType() == QueryExpression.Type.GTE) {
			return renderExpression(expression.getLeft(), tableMapping, alias, trace) + " >= " +
					renderExpression(expression.getRight(), tableMapping, alias, trace);
		}
		if (expression.getType() == QueryExpression.Type.AND) {
			return renderExpression(expression.getLeft(), tableMapping, alias, trace) + " AND " +
					renderExpression(expression.getRight(), tableMapping, alias, trace);
		}
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			ColumnMapping column = tableMapping.getRequiredColumn(expression.getColumnName());
			return renderColumn(column, tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.LITERAL) {
			return renderLiteral(expression.getLiteralValue());
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			return renderFunction(expression, tableMapping, alias, trace);
		}
		throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION, String.valueOf(expression.getType()));
	}

	private void addSelectAllProjections(TableMapping tableMapping,
										 String alias,
										 List<QueryColumn> columns,
										 List<String> projections) {
		if (tableMapping.getTableKind() == TableKind.TAG) {
			ColumnMapping idColumn = tableMapping.getIdColumn();
			projections.add("id(" + alias + ") AS " + idColumn.getSqlColumn());
			columns.add(new QueryColumn(idColumn.getSqlColumn(), idColumn.getSqlColumn(), idColumn.getJdbcType(), "VID"));
		} else {
			addEdgeEndpointProjection(projections, columns, tableMapping.getSrcColumn(), "src(" + alias + ")");
			addEdgeEndpointProjection(projections, columns, tableMapping.getDstColumn(), "dst(" + alias + ")");
			if (tableMapping.getRankColumn() != null) {
				addEdgeEndpointProjection(projections, columns, tableMapping.getRankColumn(), "rank(" + alias + ")");
			}
		}
		for (Map.Entry<String, ColumnMapping> entry : tableMapping.getProperties().entrySet()) {
			ColumnMapping column = entry.getValue();
			projections.add(alias + "." + column.getNebulaProperty() + " AS " + column.getSqlColumn());
			columns.add(new QueryColumn(column.getSqlColumn(), column.getSqlColumn(), column.getJdbcType(), "PROPERTY"));
		}
	}

	private void addExplicitProjections(NebulaQueryStatement statement,
										TableMapping tableMapping,
										String alias,
										List<String> trace,
										List<QueryColumn> columns,
										List<String> projections) throws SQLException {
		for (QueryProjection projection : statement.getProjections()) {
			QueryExpression expression = projection.getExpression();
			String label = projectionLabel(projection);
			projections.add(renderExpression(expression, tableMapping, alias, trace) + " AS " + label);
			columns.add(new QueryColumn(label, label, projectionJdbcType(expression, tableMapping), projectionSource(expression)));
		}
	}

	private String projectionLabel(QueryProjection projection) throws SQLException {
		if (projection.getAlias() != null && projection.getAlias().length() > 0) {
			return projection.getAlias();
		}
		QueryExpression expression = projection.getExpression();
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			return expression.getColumnName();
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			return normalizedFunctionName(expression.getFunctionName());
		}
		throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
				"Projection requires an alias for expression type: " + expression.getType());
	}

	private int projectionJdbcType(QueryExpression expression, TableMapping tableMapping) throws SQLException {
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			return tableMapping.getRequiredColumn(expression.getColumnName()).getJdbcType();
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			String functionName = normalizedFunctionName(expression.getFunctionName());
			if ("length".equals(functionName)) {
				return Types.INTEGER;
			}
			if ("lower".equals(functionName) || "upper".equals(functionName)) {
				return Types.VARCHAR;
			}
		}
		throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION,
				"Unsupported projection expression: " + expression.getType());
	}

	private String projectionSource(QueryExpression expression) {
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			return "PROPERTY";
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			return "FUNCTION";
		}
		return "EXPRESSION";
	}

	private String renderColumn(ColumnMapping column, TableMapping tableMapping, String alias) {
		if (column.getRole() == ColumnRole.VID) {
			return "id(" + alias + ")";
		}
		if (column.getRole() == ColumnRole.SRC_ID) {
			return "src(" + alias + ")";
		}
		if (column.getRole() == ColumnRole.DST_ID) {
			return "dst(" + alias + ")";
		}
		if (column.getRole() == ColumnRole.RANK) {
			return "rank(" + alias + ")";
		}
		return alias + "." + column.getNebulaProperty();
	}

	private String renderFunction(QueryExpression expression,
								  TableMapping tableMapping,
								  String alias,
								  List<String> trace) throws SQLException {
		String functionName = normalizedFunctionName(expression.getFunctionName());
		if (!"lower".equals(functionName) && !"upper".equals(functionName) && !"length".equals(functionName)) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
					"Function expression is not graph-mappable: " + expression.getFunctionName());
		}
		if (expression.getArguments().size() != 1) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
					"Function requires exactly one argument: " + expression.getFunctionName());
		}
		trace.add("function " + expression.getFunctionName() + " -> " + functionName);
		return functionName + "(" +
				renderExpression(expression.getArguments().get(0), tableMapping, alias, trace) + ")";
	}

	private String normalizedFunctionName(String functionName) throws SQLException {
		if (functionName == null || functionName.length() == 0) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
					"Function name must not be empty");
		}
		return functionName.toLowerCase(Locale.ENGLISH);
	}

	private void addEdgeEndpointProjection(List<String> projections, List<QueryColumn> columns,
										   ColumnMapping column, String ngqlExpression) {
		projections.add(ngqlExpression + " AS " + column.getSqlColumn());
		columns.add(new QueryColumn(column.getSqlColumn(), column.getSqlColumn(), column.getJdbcType(), column.getRole().name()));
	}

	private String renderLiteral(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
