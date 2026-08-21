package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		if (statement.getUnionLeft() != null || statement.getUnionRight() != null) {
			return convertUnion(statement, bindings, schemaMapping);
		}
		if (statement.getRelationPattern() != null) {
			return convertRelation(statement, schemaMapping);
		}
		return convertSingleTable(statement, schemaMapping);
	}

	private NgqlQueryPlan convertSingleTable(NebulaQueryStatement statement,
											 NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTableName());
		String alias = statement.getAlias();
		List<String> trace = new ArrayList<String>();
		RenderContext context = RenderContext.single(tableMapping, alias);
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
		appendWhere(ngql, statement.getWhere(), context, trace);
		ProjectionResult projectionResult = buildProjections(statement, context, trace);
		if (projectionResult.hasAggregate() || !statement.getGroupBy().isEmpty() || statement.getHaving() != null) {
			appendAggregateReturn(ngql, statement, context, trace, projectionResult);
		} else {
			appendReturn(ngql, projectionResult.getExpressions());
			appendOrderAndPagination(ngql, statement, context, trace);
		}

		QueryKind queryKind = aggregateKind(statement, projectionResult, tableMapping);
		QueryFidelity fidelity = tableMapping.getTableKind() == TableKind.TAG && queryKind != QueryKind.AGGREGATE_QUERY ?
				QueryFidelity.EQUIVALENT : QueryFidelity.CONTROLLED_EQUIVALENT;
		return new NgqlQueryPlan(ngql.toString(), queryKind, projectionResult.getColumns(), fidelity, trace);
	}

	private NgqlQueryPlan convertRelation(NebulaQueryStatement statement,
										  NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		RelationPattern relation = statement.getRelationPattern();
		TableMapping left = schemaMapping.getRequired(relation.getLeftTable());
		TableMapping edge = schemaMapping.getRequired(relation.getEdgeTable());
		TableMapping right = schemaMapping.getRequired(relation.getRightTable());
		requireKind(left, TableKind.TAG, relation.getLeftTable());
		requireKind(edge, TableKind.EDGE, relation.getEdgeTable());
		requireKind(right, TableKind.TAG, relation.getRightTable());

		List<String> trace = new ArrayList<String>();
		trace.add("join " + relation.getLeftTable() + "-" + relation.getEdgeTable() + "-" +
				relation.getRightTable() + " -> tag-edge-tag");
		RenderContext context = RenderContext.relation(relation, left, edge, right);
		StringBuilder ngql = new StringBuilder();
		ngql.append("MATCH (").append(relation.getLeftAlias()).append(":").append(left.getNebulaName())
				.append(")-[").append(relation.getEdgeAlias()).append(":").append(edge.getNebulaName())
				.append("]->(").append(relation.getRightAlias()).append(":").append(right.getNebulaName()).append(")");
		appendWhere(ngql, statement.getWhere(), context, trace);
		ProjectionResult projectionResult = buildProjections(statement, context, trace);
		if (projectionResult.getExpressions().isEmpty()) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_JOIN,
					"tag-edge-tag JOIN requires explicit projections");
		}
		appendReturn(ngql, projectionResult.getExpressions());
		return new NgqlQueryPlan(ngql.toString(), QueryKind.RELATION_QUERY, projectionResult.getColumns(),
				QueryFidelity.CONTROLLED_EQUIVALENT, trace);
	}

	private NgqlQueryPlan convertUnion(NebulaQueryStatement statement,
									   NebulaQueryBindings bindings,
									   NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		if (statement.getUnionLeft() == null || statement.getUnionRight() == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_UNION,
					"UNION requires left and right SELECT branches");
		}
		NgqlQueryPlan left = convert(statement.getUnionLeft(), bindings, schemaMapping);
		NgqlQueryPlan right = convert(statement.getUnionRight(), bindings, schemaMapping);
		validateUnionColumns(left, right);
		List<String> trace = new ArrayList<String>();
		trace.add("union " + statement.getUnionOperator());
		trace.add(left.getTrace());
		trace.add(right.getTrace());
		return new NgqlQueryPlan(left.getNgql() + " " + statement.getUnionOperator() + " " + right.getNgql(),
				QueryKind.SET_QUERY, left.getColumns(), QueryFidelity.CONTROLLED_EQUIVALENT, trace);
	}

	private void validateUnionColumns(NgqlQueryPlan left, NgqlQueryPlan right) throws SQLException {
		if (left.getColumns().size() != right.getColumns().size()) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_UNION,
					"UNION branches must expose the same column count");
		}
		for (int i = 0; i < left.getColumns().size(); i++) {
			QueryColumn leftColumn = left.getColumns().get(i);
			QueryColumn rightColumn = right.getColumns().get(i);
			if (!leftColumn.getLabel().equals(rightColumn.getLabel()) ||
					leftColumn.getJdbcType() != rightColumn.getJdbcType()) {
				throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_UNION,
						"UNION branches must expose compatible columns");
			}
		}
	}

	private QueryKind aggregateKind(NebulaQueryStatement statement, ProjectionResult projectionResult,
									TableMapping tableMapping) {
		if (projectionResult.hasAggregate() || !statement.getGroupBy().isEmpty() || statement.getHaving() != null) {
			return QueryKind.AGGREGATE_QUERY;
		}
		return tableMapping.getTableKind() == TableKind.TAG ? QueryKind.VERTEX_QUERY : QueryKind.EDGE_QUERY;
	}

	private void requireKind(TableMapping tableMapping, TableKind expected, String tableName) throws SQLException {
		if (tableMapping.getTableKind() != expected) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_JOIN,
					"JOIN table " + tableName + " must be mapped as " + expected);
		}
	}

	private void appendWhere(StringBuilder ngql, QueryExpression where,
							 RenderContext context, List<String> trace) throws SQLException {
		if (where != null) {
			String filter = renderExpression(where, context, trace);
			ngql.append(" WHERE ").append(filter);
			trace.add("filter " + filter);
		}
	}

	private void appendReturn(StringBuilder ngql, List<String> expressions) {
		ngql.append(" RETURN ");
		for (int i = 0; i < expressions.size(); i++) {
			if (i > 0) {
				ngql.append(", ");
			}
			ngql.append(expressions.get(i));
		}
	}

	private void appendAggregateReturn(StringBuilder ngql, NebulaQueryStatement statement,
									   RenderContext context, List<String> trace,
									   ProjectionResult projectionResult) throws SQLException {
		for (QueryExpression groupBy : statement.getGroupBy()) {
			trace.add("group [" + renderExpression(groupBy, context, trace) + "]");
		}
		ngql.append(" WITH ");
		for (int i = 0; i < projectionResult.getExpressions().size(); i++) {
			if (i > 0) {
				ngql.append(", ");
			}
			ngql.append(projectionResult.getExpressions().get(i));
		}
		if (statement.getHaving() != null) {
			RenderContext havingContext = context.withLabels(projectionResult.getLabels());
			ngql.append(" WHERE ").append(renderExpression(statement.getHaving(), havingContext, trace));
		}
		ngql.append(" RETURN ");
		for (int i = 0; i < projectionResult.getLabels().size(); i++) {
			if (i > 0) {
				ngql.append(", ");
			}
			ngql.append(projectionResult.getLabels().get(i));
		}
	}

	private void appendOrderAndPagination(StringBuilder ngql, NebulaQueryStatement statement,
										  RenderContext context, List<String> trace) throws SQLException {
		if (statement.getOrderByColumn() != null) {
			ColumnRender orderColumn = context.resolveColumn(null, statement.getOrderByColumn());
			ngql.append(" ORDER BY ").append(renderColumn(orderColumn.getColumn(), orderColumn.getAlias()))
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
	}

	private String renderExpression(QueryExpression expression, RenderContext context,
									List<String> trace) throws SQLException {
		if (expression.getType() == QueryExpression.Type.EQ) {
			return renderExpression(expression.getLeft(), context, trace) + " == " +
					renderExpression(expression.getRight(), context, trace);
		}
		if (expression.getType() == QueryExpression.Type.GTE) {
			return renderExpression(expression.getLeft(), context, trace) + " >= " +
					renderExpression(expression.getRight(), context, trace);
		}
		if (expression.getType() == QueryExpression.Type.AND) {
			return renderExpression(expression.getLeft(), context, trace) + " AND " +
					renderExpression(expression.getRight(), context, trace);
		}
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			if (expression.getTableName() == null && context.isLabel(expression.getColumnName())) {
				return expression.getColumnName();
			}
			ColumnRender column = context.resolveColumn(expression.getTableName(), expression.getColumnName());
			return renderColumn(column.getColumn(), column.getAlias());
		}
		if (expression.getType() == QueryExpression.Type.LITERAL) {
			return renderLiteral(expression.getLiteralValue());
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			return renderFunction(expression, context, trace);
		}
		throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION, String.valueOf(expression.getType()));
	}

	private ProjectionResult buildProjections(NebulaQueryStatement statement,
											  RenderContext context,
											  List<String> trace) throws SQLException {
		List<QueryColumn> columns = new ArrayList<QueryColumn>();
		List<String> projections = new ArrayList<String>();
		List<String> labels = new ArrayList<String>();
		boolean aggregate = false;
		if (!statement.getProjections().isEmpty() && !statement.isSelectAll()) {
			for (QueryProjection projection : statement.getProjections()) {
				QueryExpression expression = projection.getExpression();
				String label = projectionLabel(projection);
				projections.add(renderExpression(expression, context, trace) + " AS " + label);
				columns.add(new QueryColumn(label, label, projectionJdbcType(expression, context), projectionSource(expression)));
				labels.add(label);
				aggregate = aggregate || expression.isAggregate();
			}
		} else {
			addSelectAllProjections(context.getDefaultMapping(), context.getDefaultAlias(), columns, projections, labels);
		}
		trace.add("projection " + projections);
		return new ProjectionResult(projections, labels, columns, aggregate);
	}

	private void addSelectAllProjections(TableMapping tableMapping,
										 String alias,
										 List<QueryColumn> columns,
										 List<String> projections,
										 List<String> labels) {
		if (tableMapping.getTableKind() == TableKind.TAG) {
			ColumnMapping idColumn = tableMapping.getIdColumn();
			projections.add("id(" + alias + ") AS " + idColumn.getSqlColumn());
			columns.add(new QueryColumn(idColumn.getSqlColumn(), idColumn.getSqlColumn(), idColumn.getJdbcType(), "VID"));
			labels.add(idColumn.getSqlColumn());
		} else {
			addEdgeEndpointProjection(projections, labels, columns, tableMapping.getSrcColumn(), "src(" + alias + ")");
			addEdgeEndpointProjection(projections, labels, columns, tableMapping.getDstColumn(), "dst(" + alias + ")");
			if (tableMapping.getRankColumn() != null) {
				addEdgeEndpointProjection(projections, labels, columns, tableMapping.getRankColumn(), "rank(" + alias + ")");
			}
		}
		for (Map.Entry<String, ColumnMapping> entry : tableMapping.getProperties().entrySet()) {
			ColumnMapping column = entry.getValue();
			projections.add(alias + "." + column.getNebulaProperty() + " AS " + column.getSqlColumn());
			columns.add(new QueryColumn(column.getSqlColumn(), column.getSqlColumn(), column.getJdbcType(), "PROPERTY"));
			labels.add(column.getSqlColumn());
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

	private int projectionJdbcType(QueryExpression expression, RenderContext context) throws SQLException {
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			return context.resolveColumn(expression.getTableName(), expression.getColumnName()).getColumn().getJdbcType();
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			String functionName = normalizedFunctionName(expression.getFunctionName());
			if (expression.isAggregate() && "count".equals(functionName)) {
				return Types.BIGINT;
			}
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
		if (expression.getType() == QueryExpression.Type.FUNCTION && expression.isAggregate()) {
			return "AGGREGATE";
		}
		if (expression.getType() == QueryExpression.Type.FUNCTION) {
			return "FUNCTION";
		}
		return "EXPRESSION";
	}

	private String renderColumn(ColumnMapping column, String alias) {
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
								  RenderContext context,
								  List<String> trace) throws SQLException {
		String functionName = normalizedFunctionName(expression.getFunctionName());
		if (expression.isAggregate()) {
			if (!"count".equals(functionName)) {
				throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
						"Aggregate function is not graph-mappable: " + expression.getFunctionName());
			}
			if (expression.getArguments().size() != 1) {
				throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
						"Aggregate function requires exactly one argument: " + expression.getFunctionName());
			}
			trace.add("aggregate " + expression.getFunctionName() + " -> " + functionName);
			return functionName + "(" + renderExpression(expression.getArguments().get(0), context, trace) + ")";
		}
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
				renderExpression(expression.getArguments().get(0), context, trace) + ")";
	}

	private String normalizedFunctionName(String functionName) throws SQLException {
		if (functionName == null || functionName.length() == 0) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION,
					"Function name must not be empty");
		}
		return functionName.toLowerCase(Locale.ENGLISH);
	}

	private void addEdgeEndpointProjection(List<String> projections, List<String> labels, List<QueryColumn> columns,
										   ColumnMapping column, String ngqlExpression) {
		projections.add(ngqlExpression + " AS " + column.getSqlColumn());
		columns.add(new QueryColumn(column.getSqlColumn(), column.getSqlColumn(), column.getJdbcType(), column.getRole().name()));
		labels.add(column.getSqlColumn());
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

	private static class ProjectionResult {
		private final List<String> expressions;
		private final List<String> labels;
		private final List<QueryColumn> columns;
		private final boolean aggregate;

		private ProjectionResult(List<String> expressions, List<String> labels,
								 List<QueryColumn> columns, boolean aggregate) {
			this.expressions = expressions;
			this.labels = labels;
			this.columns = columns;
			this.aggregate = aggregate;
		}

		private List<String> getExpressions() {
			return expressions;
		}

		private List<String> getLabels() {
			return labels;
		}

		private List<QueryColumn> getColumns() {
			return columns;
		}

		private boolean hasAggregate() {
			return aggregate;
		}
	}

	private static class RenderContext {
		private final String defaultAlias;
		private final TableMapping defaultMapping;
		private final Map<String, AliasMapping> aliases;
		private final List<String> labels;

		private RenderContext(String defaultAlias, TableMapping defaultMapping,
							  Map<String, AliasMapping> aliases, List<String> labels) {
			this.defaultAlias = defaultAlias;
			this.defaultMapping = defaultMapping;
			this.aliases = aliases;
			this.labels = labels;
		}

		private static RenderContext single(TableMapping mapping, String alias) {
			Map<String, AliasMapping> aliases = new LinkedHashMap<String, AliasMapping>();
			aliases.put(alias, new AliasMapping(alias, mapping));
			aliases.put(mapping.getSqlTable(), new AliasMapping(alias, mapping));
			return new RenderContext(alias, mapping, aliases, new ArrayList<String>());
		}

		private static RenderContext relation(RelationPattern relation, TableMapping left,
											  TableMapping edge, TableMapping right) {
			Map<String, AliasMapping> aliases = new LinkedHashMap<String, AliasMapping>();
			aliases.put(relation.getLeftAlias(), new AliasMapping(relation.getLeftAlias(), left));
			aliases.put(relation.getEdgeAlias(), new AliasMapping(relation.getEdgeAlias(), edge));
			aliases.put(relation.getRightAlias(), new AliasMapping(relation.getRightAlias(), right));
			return new RenderContext(relation.getLeftAlias(), left, aliases, new ArrayList<String>());
		}

		private RenderContext withLabels(List<String> projectionLabels) {
			return new RenderContext(defaultAlias, defaultMapping, aliases, projectionLabels);
		}

		private ColumnRender resolveColumn(String qualifier, String columnName) throws SQLException {
			AliasMapping mapping = qualifier == null ? aliases.get(defaultAlias) : aliases.get(qualifier);
			if (mapping == null) {
				throw NebulaQueryConversionException.of(NebulaQueryErrorCode.MISSING_TABLE_MAPPING, qualifier);
			}
			return new ColumnRender(mapping.getAlias(), mapping.getMapping().getRequiredColumn(columnName));
		}

		private boolean isLabel(String columnName) {
			return labels.contains(columnName);
		}

		private String getDefaultAlias() {
			return defaultAlias;
		}

		private TableMapping getDefaultMapping() {
			return defaultMapping;
		}
	}

	private static class AliasMapping {
		private final String alias;
		private final TableMapping mapping;

		private AliasMapping(String alias, TableMapping mapping) {
			this.alias = alias;
			this.mapping = mapping;
		}

		private String getAlias() {
			return alias;
		}

		private TableMapping getMapping() {
			return mapping;
		}
	}

	private static class ColumnRender {
		private final String alias;
		private final ColumnMapping column;

		private ColumnRender(String alias, ColumnMapping column) {
			this.alias = alias;
			this.column = column;
		}

		private String getAlias() {
			return alias;
		}

		private ColumnMapping getColumn() {
			return column;
		}
	}
}
