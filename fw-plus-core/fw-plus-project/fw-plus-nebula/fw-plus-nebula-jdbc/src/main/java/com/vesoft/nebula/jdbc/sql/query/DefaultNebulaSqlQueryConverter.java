package com.vesoft.nebula.jdbc.sql.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
			String filter = renderExpression(statement.getWhere(), tableMapping, alias);
			ngql.append(" WHERE ").append(filter);
			trace.add("filter " + filter);
		}
		ngql.append(" RETURN ");

		List<QueryColumn> columns = new ArrayList<QueryColumn>();
		List<String> projections = new ArrayList<String>();
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

	private String renderExpression(QueryExpression expression, TableMapping tableMapping, String alias) throws SQLException {
		if (expression.getType() == QueryExpression.Type.EQ) {
			return renderExpression(expression.getLeft(), tableMapping, alias) + " == " +
					renderExpression(expression.getRight(), tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.GTE) {
			return renderExpression(expression.getLeft(), tableMapping, alias) + " >= " +
					renderExpression(expression.getRight(), tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.AND) {
			return renderExpression(expression.getLeft(), tableMapping, alias) + " AND " +
					renderExpression(expression.getRight(), tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			ColumnMapping column = tableMapping.getRequiredColumn(expression.getColumnName());
			return renderColumn(column, tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.LITERAL) {
			return renderLiteral(expression.getLiteralValue());
		}
		throw NebulaQueryConversionException.of(NebulaQueryErrorCode.UNSUPPORTED_EXPRESSION, String.valueOf(expression.getType()));
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
