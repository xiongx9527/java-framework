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
			throw new SQLException("NEBULA_SQL_QUERY_INVALID_INPUT: statement must not be null");
		}
		if (statement.getKind() != StatementKind.SELECT) {
			throw new SQLException("NEBULA_SQL_QUERY_ONLY: SQL query converter only supports SELECT statements");
		}
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTableName());
		if (tableMapping.getTableKind() != TableKind.TAG) {
			throw new SQLException("NEBULA_SQL_QUERY_UNSUPPORTED_TABLE_KIND: expected TAG mapping for table " + statement.getTableName());
		}
		String alias = statement.getAlias();
		List<String> trace = new ArrayList<String>();
		trace.add("table " + statement.getTableName() + " -> TAG " + tableMapping.getNebulaName());

		StringBuilder ngql = new StringBuilder();
		ngql.append("MATCH (").append(alias).append(":").append(tableMapping.getNebulaName()).append(")");
		if (statement.getWhere() != null) {
			String filter = renderExpression(statement.getWhere(), tableMapping, alias);
			ngql.append(" WHERE ").append(filter);
			trace.add("filter " + filter);
		}
		ngql.append(" RETURN ");

		List<QueryColumn> columns = new ArrayList<QueryColumn>();
		List<String> projections = new ArrayList<String>();
		ColumnMapping idColumn = tableMapping.getIdColumn();
		projections.add("id(" + alias + ") AS " + idColumn.getSqlColumn());
		columns.add(new QueryColumn(idColumn.getSqlColumn(), idColumn.getSqlColumn(), idColumn.getJdbcType(), "VID"));
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

		return new NgqlQueryPlan(ngql.toString(), QueryKind.VERTEX_QUERY, columns, QueryFidelity.EQUIVALENT, trace);
	}

	private String renderExpression(QueryExpression expression, TableMapping tableMapping, String alias) throws SQLException {
		if (expression.getType() == QueryExpression.Type.EQ) {
			return renderExpression(expression.getLeft(), tableMapping, alias) + " == " +
					renderExpression(expression.getRight(), tableMapping, alias);
		}
		if (expression.getType() == QueryExpression.Type.COLUMN) {
			ColumnMapping column = tableMapping.getRequiredColumn(expression.getColumnName());
			if (column.getRole() == ColumnRole.VID) {
				return "id(" + alias + ")";
			}
			return alias + "." + column.getNebulaProperty();
		}
		if (expression.getType() == QueryExpression.Type.LITERAL) {
			return renderLiteral(expression.getLiteralValue());
		}
		throw new SQLException("NEBULA_SQL_QUERY_UNSUPPORTED_EXPRESSION: " + expression.getType());
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
