package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaExpression;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaInsertStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.query.ColumnMapping;
import com.vesoft.nebula.jdbc.sql.query.ColumnRole;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.TableKind;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultNebulaSqlWriteConverter implements NebulaSqlWriteConverter {

	public NebulaMutationPlan convert(NebulaSqlStatement statement, NebulaWriteBindings bindings,
			NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		if (statement instanceof NebulaInsertStatement) {
			return convertInsert((NebulaInsertStatement) statement, bindings, schemaMapping);
		}
		throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT,
				"Only INSERT is supported by this converter step.");
	}

	private NebulaMutationPlan convertInsert(NebulaInsertStatement statement, NebulaWriteBindings bindings,
			NebulaSchemaMappingRegistry schemaMapping) throws SQLException {
		if (statement.getSelectSource() != null) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT,
					"INSERT ... SELECT is not supported.");
		}
		TableMapping tableMapping = schemaMapping.getRequired(statement.getTableName());
		List<String> trace = new ArrayList<String>();
		trace.add("table " + tableMapping.getSqlTable() + " -> " + tableMapping.getTableKind() + " " + tableMapping.getNebulaName());
		if (tableMapping.getTableKind() == TableKind.TAG) {
			return convertInsertVertex(statement, bindings, tableMapping, trace);
		}
		return convertInsertEdge(statement, bindings, tableMapping, trace);
	}

	private NebulaMutationPlan convertInsertVertex(NebulaInsertStatement statement, NebulaWriteBindings bindings,
			TableMapping tableMapping, List<String> trace) throws SQLException {
		if (tableMapping.getIdColumn() == null) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_MISSING_ID,
					"Tag mapping requires an id column.");
		}
		RowValues row = singleRow(statement);
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		List<String> propertyNames = new ArrayList<String>();
		List<String> propertyValues = new ArrayList<String>();
		for (int i = 0; i < statement.getColumns().size(); i++) {
			String column = statement.getColumns().get(i);
			ColumnMapping columnMapping = tableMapping.getRequiredColumn(column);
			Object value = row.getValues().get(i);
			if (columnMapping.getRole() == ColumnRole.VID) {
				keys.put(column, value);
			} else if (columnMapping.getRole() == ColumnRole.PROPERTY) {
				properties.put(column, value);
				propertyNames.add(columnMapping.getNebulaProperty());
				propertyValues.add(toNgqlLiteral(value));
			}
		}
		Object id = keys.get(tableMapping.getIdColumn().getSqlColumn());
		if (id == null) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_MISSING_ID,
					"INSERT tag requires id column " + tableMapping.getIdColumn().getSqlColumn() + ".");
		}
		String ngql = "INSERT VERTEX " + tableMapping.getNebulaName() + "(" + join(propertyNames)
				+ ") VALUES " + toNgqlLiteral(id) + ":(" + join(propertyValues) + ")";
		return plan(NebulaMutationKind.INSERT_VERTEX, tableMapping, keys, properties, bindings, ngql, trace);
	}

	private NebulaMutationPlan convertInsertEdge(NebulaInsertStatement statement, NebulaWriteBindings bindings,
			TableMapping tableMapping, List<String> trace) throws SQLException {
		if (tableMapping.getSrcColumn() == null || tableMapping.getDstColumn() == null) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_MISSING_ID,
					"Edge mapping requires src and dst columns.");
		}
		RowValues row = singleRow(statement);
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		List<String> propertyNames = new ArrayList<String>();
		List<String> propertyValues = new ArrayList<String>();
		for (int i = 0; i < statement.getColumns().size(); i++) {
			String column = statement.getColumns().get(i);
			ColumnMapping columnMapping = tableMapping.getRequiredColumn(column);
			Object value = row.getValues().get(i);
			if (columnMapping.getRole() == ColumnRole.SRC_ID
					|| columnMapping.getRole() == ColumnRole.DST_ID
					|| columnMapping.getRole() == ColumnRole.RANK) {
				keys.put(column, value);
			} else if (columnMapping.getRole() == ColumnRole.PROPERTY) {
				properties.put(column, value);
				propertyNames.add(columnMapping.getNebulaProperty());
				propertyValues.add(toNgqlLiteral(value));
			}
		}
		Object src = keys.get(tableMapping.getSrcColumn().getSqlColumn());
		Object dst = keys.get(tableMapping.getDstColumn().getSqlColumn());
		if (src == null || dst == null) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_MISSING_ID,
					"INSERT edge requires src and dst columns.");
		}
		String rank = "";
		if (tableMapping.getRankColumn() != null && keys.containsKey(tableMapping.getRankColumn().getSqlColumn())) {
			rank = "@" + toNgqlLiteral(keys.get(tableMapping.getRankColumn().getSqlColumn()));
		}
		String ngql = "INSERT EDGE " + tableMapping.getNebulaName() + "(" + join(propertyNames)
				+ ") VALUES " + toNgqlLiteral(src) + "->" + toNgqlLiteral(dst) + rank
				+ ":(" + join(propertyValues) + ")";
		return plan(NebulaMutationKind.INSERT_EDGE, tableMapping, keys, properties, bindings, ngql, trace);
	}

	private NebulaMutationPlan plan(NebulaMutationKind kind, TableMapping tableMapping, Map<String, Object> keys,
			Map<String, Object> properties, NebulaWriteBindings bindings, String ngql, List<String> trace) {
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(kind, tableMapping.getNebulaName(),
				keys, properties, bindings == null ? null : bindings.getSourceSql(), ngql);
		return new NebulaMutationPlan(kind, ngql, tableMapping.getNebulaName(), keys, properties,
				"single-plan-success", descriptor, trace);
	}

	private RowValues singleRow(NebulaInsertStatement statement) throws SQLException {
		if (statement.getValues().size() != 1) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT,
					"Only single-row INSERT is supported in this step.");
		}
		List<NebulaExpression> row = statement.getValues().get(0);
		if (row.size() != statement.getColumns().size()) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT,
					"INSERT column count does not match values count.");
		}
		List<Object> values = new ArrayList<Object>();
		for (NebulaExpression expression : row) {
			values.add(toValue(expression));
		}
		return new RowValues(values);
	}

	private Object toValue(NebulaExpression expression) throws SQLException {
		if (expression.getType() == NebulaExpression.Type.LITERAL) {
			return expression.getValue();
		}
		throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSUPPORTED_STATEMENT,
				"Only literal INSERT values are supported in this step.");
	}

	private String toNgqlLiteral(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private String join(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(values.get(i));
		}
		return builder.toString();
	}

	private static class RowValues {
		private final List<Object> values;

		private RowValues(List<Object> values) {
			this.values = values;
		}

		public List<Object> getValues() {
			return values;
		}
	}
}
