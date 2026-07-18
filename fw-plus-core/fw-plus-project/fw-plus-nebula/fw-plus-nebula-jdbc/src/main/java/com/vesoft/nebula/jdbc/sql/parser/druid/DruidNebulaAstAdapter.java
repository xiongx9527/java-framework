package com.vesoft.nebula.jdbc.sql.parser.druid;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParameter;
import com.vesoft.nebula.jdbc.sql.parser.StatementKind;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaColumnDefinition;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaDeleteStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaExpression;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaInsertStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaLimit;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaMetadataStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaOrderByItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSchemaStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaTableSource;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaUnion;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaUpdateItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaUpdateStatement;
import com.vesoft.nebula.jdbc.sql.parser.error.ErrorKind;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseError;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DruidNebulaAstAdapter {

	public AdaptResult adapt(SQLStatement statement, String sql) throws NebulaSqlParseException {
		AdaptContext context = new AdaptContext(sql);
		NebulaSqlStatement adapted = adaptStatement(statement, context);
		return new AdaptResult(adapted, context.parameters, null);
	}

	private NebulaSqlStatement adaptStatement(SQLStatement statement, AdaptContext context) throws NebulaSqlParseException {
		String simpleName = statement.getClass().getSimpleName();
		String normalized = statement.toString().trim().toUpperCase();
		if (simpleName.indexOf("Call") >= 0 || normalized.startsWith("CALL ")) {
			throw context.error(ErrorKind.UNSUPPORTED_FEATURE, "Stored procedure calls are not supported",
					"SQL-UNSUPPORTED-PROCEDURE", statement.getClass().getName());
		}
		if (simpleName.indexOf("Trigger") >= 0 || normalized.startsWith("CREATE TRIGGER")) {
			throw context.error(ErrorKind.UNSUPPORTED_FEATURE, "Trigger DDL is handled by the trigger DDL handler",
					"SQL-UNSUPPORTED-TRIGGER-DDL", statement.getClass().getName());
		}
		if (statement instanceof SQLSelectStatement) {
			return adaptSelect(((SQLSelectStatement) statement).getSelect(), context);
		}
		if (statement instanceof SQLInsertStatement) {
			return adaptInsert((SQLInsertStatement) statement, context);
		}
		if (statement instanceof SQLUpdateStatement) {
			return adaptUpdate((SQLUpdateStatement) statement, context);
		}
		if (statement instanceof SQLDeleteStatement) {
			return adaptDelete((SQLDeleteStatement) statement, context);
		}
		if (statement instanceof SQLCreateTableStatement) {
			return adaptCreateTable((SQLCreateTableStatement) statement, context);
		}
		if (statement instanceof SQLDropTableStatement) {
			return adaptDropTable((SQLDropTableStatement) statement, context);
		}
		if (simpleName.indexOf("Show") >= 0 || simpleName.indexOf("Describe") >= 0 || normalized.startsWith("DESC")) {
			return adaptMetadata(statement);
		}
		throw context.error(ErrorKind.ADAPTER_GAP, "Unsupported Druid statement node: " + statement.getClass().getName(),
				null, statement.getClass().getName());
	}

	private NebulaInsertStatement adaptInsert(SQLInsertStatement statement, AdaptContext context) throws NebulaSqlParseException {
		List<String> columns = new ArrayList<String>();
		for (SQLExpr column : statement.getColumns()) {
			columns.add(nameOf(column));
		}
		List<List<NebulaExpression>> values = new ArrayList<List<NebulaExpression>>();
		for (SQLInsertStatement.ValuesClause clause : statement.getValuesList()) {
			List<NebulaExpression> row = new ArrayList<NebulaExpression>();
			for (SQLExpr value : clause.getValues()) {
				row.add(adaptExpression(value, context));
			}
			values.add(row);
		}
		NebulaSelectStatement selectSource = statement.getQuery() == null ? null : adaptSelect(statement.getQuery(), context);
		return new NebulaInsertStatement(tableName(statement.getTableSource()), columns, values, selectSource);
	}

	private NebulaUpdateStatement adaptUpdate(SQLUpdateStatement statement, AdaptContext context) throws NebulaSqlParseException {
		List<NebulaUpdateItem> items = new ArrayList<NebulaUpdateItem>();
		for (SQLUpdateSetItem item : statement.getItems()) {
			items.add(new NebulaUpdateItem(nameOf(item.getColumn()), adaptExpression(item.getValue(), context)));
		}
		return new NebulaUpdateStatement(adaptTableSource(statement.getTableSource(), context), items,
				adaptNullableExpression(statement.getWhere(), context));
	}

	private NebulaDeleteStatement adaptDelete(SQLDeleteStatement statement, AdaptContext context) throws NebulaSqlParseException {
		return new NebulaDeleteStatement(adaptTableSource(statement.getTableSource(), context),
				adaptNullableExpression(statement.getWhere(), context));
	}

	private NebulaSchemaStatement adaptCreateTable(SQLCreateTableStatement statement, AdaptContext context) {
		List<NebulaColumnDefinition> columns = new ArrayList<NebulaColumnDefinition>();
		for (SQLTableElement element : statement.getTableElementList()) {
			if (element instanceof SQLColumnDefinition) {
				SQLColumnDefinition column = (SQLColumnDefinition) element;
				columns.add(new NebulaColumnDefinition(column.getNameAsString(),
						column.getDataType() == null ? null : column.getDataType().toString(),
						column.isPrimaryKey()));
			}
		}
		return new NebulaSchemaStatement(StatementKind.CREATE_SCHEMA, nameOf(statement.getName()), columns);
	}

	private NebulaSchemaStatement adaptDropTable(SQLDropTableStatement statement, AdaptContext context) {
		String tableName = statement.getTableSources().isEmpty() ? null : tableName(statement.getTableSources().get(0));
		return new NebulaSchemaStatement(StatementKind.DROP_SCHEMA, tableName, Collections.<NebulaColumnDefinition>emptyList());
	}

	private NebulaMetadataStatement adaptMetadata(SQLStatement statement) {
		String simpleName = statement.getClass().getSimpleName();
		String metadataType = simpleName;
		String sql = statement.toString();
		String targetName = null;
		String upper = sql.toUpperCase();
		int from = upper.indexOf(" FROM ");
		if (from >= 0) {
			targetName = sql.substring(from + 6).trim();
		}
		else {
			String[] parts = sql.trim().split("\\s+");
			if (parts.length > 1 && ("DESC".equalsIgnoreCase(parts[0]) || "DESCRIBE".equalsIgnoreCase(parts[0]))) {
				targetName = parts[1];
			}
		}
		return new NebulaMetadataStatement(metadataType, targetName);
	}

	private NebulaSelectStatement adaptSelect(SQLSelect select, AdaptContext context) throws NebulaSqlParseException {
		if (select == null) {
			throw context.error(ErrorKind.ADAPTER_GAP, "SELECT statement has no query", null, null);
		}
		return adaptSelectQuery(select.getQuery(), select.getOrderBy(), context);
	}

	private NebulaSelectStatement adaptSelectQuery(SQLSelectQuery query, SQLOrderBy inheritedOrderBy, AdaptContext context)
			throws NebulaSqlParseException {
		if (query instanceof SQLSelectQueryBlock) {
			return adaptQueryBlock((SQLSelectQueryBlock) query, inheritedOrderBy, null, context);
		}
		if (query instanceof SQLUnionQuery) {
			SQLUnionQuery unionQuery = (SQLUnionQuery) query;
			NebulaSelectStatement left = adaptSelectQuery(unionQuery.getLeft(), null, context);
			NebulaSelectStatement right = adaptSelectQuery(unionQuery.getRight(), null, context);
			NebulaUnion union = new NebulaUnion(left, right, String.valueOf(unionQuery.getOperator()));
			return new NebulaSelectStatement(Collections.<NebulaSelectItem>emptyList(), null, null,
					Collections.<NebulaExpression>emptyList(), null, Collections.<NebulaOrderByItem>emptyList(), null, union);
		}
		throw context.error(ErrorKind.ADAPTER_GAP, "Unsupported SELECT query node: " + query.getClass().getName(),
				null, query.getClass().getName());
	}

	private NebulaSelectStatement adaptQueryBlock(SQLSelectQueryBlock query, SQLOrderBy inheritedOrderBy, NebulaUnion union,
												 AdaptContext context) throws NebulaSqlParseException {
		List<NebulaSelectItem> selectItems = new ArrayList<NebulaSelectItem>();
		for (SQLSelectItem item : query.getSelectList()) {
			selectItems.add(new NebulaSelectItem(adaptExpression(item.getExpr(), context), item.getAlias()));
		}
		SQLSelectGroupByClause groupByClause = query.getGroupBy();
		List<NebulaExpression> groupBy = new ArrayList<NebulaExpression>();
		NebulaExpression having = null;
		if (groupByClause != null) {
			for (SQLExpr item : groupByClause.getItems()) {
				groupBy.add(adaptExpression(item, context));
			}
			having = adaptNullableExpression(groupByClause.getHaving(), context);
		}
		SQLOrderBy orderBy = query.getOrderBy() == null ? inheritedOrderBy : query.getOrderBy();
		return new NebulaSelectStatement(selectItems, adaptNullableTableSource(query.getFrom(), context),
				adaptNullableExpression(query.getWhere(), context), groupBy, having, adaptOrderBy(orderBy, context),
				adaptLimit(query.getLimit()), union);
	}

	private List<NebulaOrderByItem> adaptOrderBy(SQLOrderBy orderBy, AdaptContext context) throws NebulaSqlParseException {
		List<NebulaOrderByItem> items = new ArrayList<NebulaOrderByItem>();
		if (orderBy == null) {
			return items;
		}
		for (SQLSelectOrderByItem item : orderBy.getItems()) {
			SQLOrderingSpecification type = item.getType();
			items.add(new NebulaOrderByItem(adaptExpression(item.getExpr(), context), type == null ? null : type.name()));
		}
		return items;
	}

	private NebulaLimit adaptLimit(SQLLimit limit) {
		if (limit == null) {
			return null;
		}
		return new NebulaLimit(integerValue(limit.getOffset()), integerValue(limit.getRowCount()));
	}

	private NebulaTableSource adaptNullableTableSource(SQLTableSource source, AdaptContext context) throws NebulaSqlParseException {
		return source == null ? null : adaptTableSource(source, context);
	}

	private NebulaTableSource adaptTableSource(SQLTableSource source, AdaptContext context) throws NebulaSqlParseException {
		if (source instanceof SQLExprTableSource) {
			SQLExprTableSource table = (SQLExprTableSource) source;
			return NebulaTableSource.table(nameOf(table.getExpr()), table.getAlias());
		}
		if (source instanceof SQLJoinTableSource) {
			SQLJoinTableSource join = (SQLJoinTableSource) source;
			return NebulaTableSource.join(adaptTableSource(join.getLeft(), context), adaptTableSource(join.getRight(), context),
					String.valueOf(join.getJoinType()), adaptNullableExpression(join.getCondition(), context));
		}
		if (source instanceof SQLSubqueryTableSource) {
			SQLSubqueryTableSource subquery = (SQLSubqueryTableSource) source;
			return NebulaTableSource.subquery(adaptSelect(subquery.getSelect(), context), subquery.getAlias());
		}
		throw context.error(ErrorKind.ADAPTER_GAP, "Unsupported table source node: " + source.getClass().getName(),
				null, source.getClass().getName());
	}

	private NebulaExpression adaptNullableExpression(SQLExpr expression, AdaptContext context) throws NebulaSqlParseException {
		return expression == null ? null : adaptExpression(expression, context);
	}

	private NebulaExpression adaptExpression(SQLExpr expression, AdaptContext context) throws NebulaSqlParseException {
		if (expression instanceof SQLAllColumnExpr) {
			return NebulaExpression.wildcard();
		}
		if (expression instanceof SQLIdentifierExpr || expression instanceof SQLPropertyExpr) {
			return NebulaExpression.column(nameOf(expression));
		}
		if (expression instanceof SQLVariantRefExpr) {
			return NebulaExpression.parameter(context.nextParameter());
		}
		if (expression instanceof SQLCharExpr) {
			return NebulaExpression.literal(((SQLCharExpr) expression).getText());
		}
		if (expression instanceof SQLNCharExpr) {
			return NebulaExpression.literal(((SQLNCharExpr) expression).getText());
		}
		if (expression instanceof SQLIntegerExpr) {
			return NebulaExpression.literal(((SQLIntegerExpr) expression).getNumber());
		}
		if (expression instanceof SQLNumberExpr) {
			return NebulaExpression.literal(((SQLNumberExpr) expression).getNumber());
		}
		if (expression instanceof SQLBooleanExpr) {
			return NebulaExpression.literal(((SQLBooleanExpr) expression).getValue());
		}
		if (expression instanceof SQLNullExpr) {
			return NebulaExpression.literal(null);
		}
		if (expression instanceof SQLBinaryOpExpr) {
			SQLBinaryOpExpr binary = (SQLBinaryOpExpr) expression;
			String operator = binary.getOperator().getName();
			if ("LIKE".equalsIgnoreCase(operator) || "NOT LIKE".equalsIgnoreCase(operator)) {
				List<NebulaExpression> arguments = new ArrayList<NebulaExpression>();
				arguments.add(adaptExpression(binary.getRight(), context));
				return NebulaExpression.listExpression(NebulaExpression.Type.LIKE, operator,
						adaptExpression(binary.getLeft(), context), arguments);
			}
			if (("IS".equalsIgnoreCase(operator) || "IS NOT".equalsIgnoreCase(operator))
					&& binary.getRight() instanceof SQLNullExpr) {
				return NebulaExpression.listExpression(NebulaExpression.Type.IS_NULL, operator,
						adaptExpression(binary.getLeft(), context), Collections.<NebulaExpression>emptyList());
			}
			return NebulaExpression.binary(binary.getOperator().getName(),
					adaptExpression(binary.getLeft(), context), adaptExpression(binary.getRight(), context));
		}
		if (expression instanceof SQLAggregateExpr) {
			SQLAggregateExpr aggregate = (SQLAggregateExpr) expression;
			return NebulaExpression.function(aggregate.getMethodName(), adaptArguments(aggregate.getArguments(), context), true);
		}
		if (expression instanceof SQLMethodInvokeExpr) {
			SQLMethodInvokeExpr method = (SQLMethodInvokeExpr) expression;
			return NebulaExpression.function(method.getMethodName(), adaptArguments(method.getArguments(), context), false);
		}
		if (expression instanceof SQLInListExpr) {
			SQLInListExpr in = (SQLInListExpr) expression;
			return NebulaExpression.listExpression(NebulaExpression.Type.IN_LIST, in.isNot() ? "NOT IN" : "IN",
					adaptExpression(in.getExpr(), context), adaptArguments(in.getTargetList(), context));
		}
		if (expression instanceof SQLUnaryExpr) {
			SQLUnaryExpr unary = (SQLUnaryExpr) expression;
			return NebulaExpression.unary(String.valueOf(unary.getOperator()), adaptExpression(unary.getExpr(), context));
		}
		if (expression instanceof SQLValuableExpr) {
			return NebulaExpression.literal(((SQLValuableExpr) expression).getValue());
		}
		throw context.error(ErrorKind.ADAPTER_GAP, "Unsupported expression node: " + expression.getClass().getName(),
				null, expression.getClass().getName());
	}

	private List<NebulaExpression> adaptArguments(List<? extends SQLExpr> arguments, AdaptContext context) throws NebulaSqlParseException {
		List<NebulaExpression> result = new ArrayList<NebulaExpression>();
		for (SQLExpr argument : arguments) {
			result.add(adaptExpression(argument, context));
		}
		return result;
	}

	private static String tableName(SQLTableSource source) {
		if (source instanceof SQLExprTableSource) {
			return nameOf(((SQLExprTableSource) source).getExpr());
		}
		return source == null ? null : source.toString();
	}

	private static String nameOf(SQLExpr expression) {
		if (expression instanceof SQLPropertyExpr) {
			return expression.toString();
		}
		if (expression instanceof SQLName) {
			return ((SQLName) expression).getSimpleName();
		}
		return expression == null ? null : expression.toString();
	}

	private static Integer integerValue(SQLExpr expression) {
		if (expression instanceof SQLIntegerExpr) {
			return ((SQLIntegerExpr) expression).getNumber().intValue();
		}
		return null;
	}

	private static class AdaptContext {
		private final String sql;
		private final List<NebulaSqlParameter> parameters = new ArrayList<NebulaSqlParameter>();

		private AdaptContext(String sql) {
			this.sql = sql;
		}

		private int nextParameter() {
			int index = parameters.size() + 1;
			parameters.add(new NebulaSqlParameter(index));
			return index;
		}

		private NebulaSqlParseException error(ErrorKind kind, String message, String matrixId, String druidAstType) {
			return new NebulaSqlParseException(new NebulaSqlParseError(kind, message, matrixId, druidAstType,
					null, null, SqlDiagnostics.snippet(sql)));
		}
	}

	public static class AdaptResult {
		private final NebulaSqlStatement statement;
		private final List<NebulaSqlParameter> parameters;
		private final String matrixId;

		private AdaptResult(NebulaSqlStatement statement, List<NebulaSqlParameter> parameters, String matrixId) {
			this.statement = statement;
			this.parameters = Collections.unmodifiableList(new ArrayList<NebulaSqlParameter>(parameters));
			this.matrixId = matrixId;
		}

		public NebulaSqlStatement getStatement() {
			return statement;
		}

		public List<NebulaSqlParameter> getParameters() {
			return parameters;
		}

		public String getMatrixId() {
			return matrixId;
		}
	}
}
