package com.xiongx9527.fw.plus.tinyengine.dynamic.sql;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicColumnDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DynamicSqlProvider {

	public String select(Map<String, Object> params) {
		String tableName = tableName(params);
		List<String> fields = fields(params.get("fields"), true);
		List<String> conditions = conditionFields(params.get("conditions"));
		String orderBy = optionalOrderBy(params.get("orderBy"));
		TinyEngineSortDirection orderType = orderBy == null ? null : TinyEngineSqlIdentifierValidator.validateSortDirection(stringValue(params.get("orderType"), "order type"));
		boolean paged = params.containsKey("offset") || params.containsKey("pageSize");
		if (paged && (!params.containsKey("offset") || !params.containsKey("pageSize"))) {
			throw new IllegalArgumentException("Both offset and page size are required for pagination");
		}

		StringBuilder sql = new StringBuilder("SELECT ");
		sql.append(join(fields, ", ")).append(" FROM ").append(tableName);
		appendConditions(sql, conditions);
		if (orderBy != null) {
			sql.append(" ORDER BY ").append(orderBy).append(' ').append(orderType.name());
		}
		if (paged) {
			sql.append(" LIMIT #{offset}, #{pageSize}");
		}
		return sql.toString();
	}

	public String count(Map<String, Object> params) {
		String tableName = tableName(params);
		List<String> conditions = conditionFields(params.get("conditions"));
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
		sql.append(tableName);
		appendConditions(sql, conditions);
		return sql.toString();
	}

	public String insert(Map<String, Object> params) {
		String tableName = tableName(params);
		List<String> dataFields = dataFields(params.get("data"));
		return insertSql(tableName, dataFields, "data", null);
	}

	public String update(Map<String, Object> params) {
		String tableName = tableName(params);
		List<String> dataFields = dataFields(params.get("data"));
		List<String> conditions = conditionFields(params.get("conditions"));
		StringBuilder sql = new StringBuilder("UPDATE ");
		sql.append(tableName).append(" SET ");
		for (int i = 0; i < dataFields.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			String field = dataFields.get(i);
			sql.append(field).append(" = #{data.").append(field).append('}');
		}
		appendConditions(sql, conditions);
		return sql.toString();
	}

	public String delete(Map<String, Object> params) {
		String tableName = tableName(params);
		List<String> conditions = conditionFields(params.get("conditions"));
		StringBuilder sql = new StringBuilder("DELETE FROM ");
		sql.append(tableName);
		appendConditions(sql, conditions);
		return sql.toString();
	}

	public String createTable(DynamicTableDefinition definition) {
		String tableName = validatedTableName(definition == null ? null : definition.getTableName());
		List<ValidatedColumn> columns = validatedColumns(definition == null ? null : definition.getColumns(), "table definition");
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sql.append(tableName).append(" (");
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			appendColumn(sql, columns.get(i));
		}
		sql.append(')');
		return sql.toString();
	}

	public String alterTable(DynamicTableAlteration alteration) {
		String tableName = validatedTableName(alteration == null ? null : alteration.getTableName());
		List<ValidatedColumn> addColumns = validatedColumns(alteration == null ? null : alteration.getAddColumns(), "added column", true);
		List<String> dropColumns = fields(alteration == null ? null : alteration.getDropColumns(), false);
		if (addColumns.isEmpty() && dropColumns.isEmpty()) {
			throw new IllegalArgumentException("Table alteration must contain a change");
		}
		StringBuilder sql = new StringBuilder("ALTER TABLE ");
		sql.append(tableName).append(' ');
		boolean hasPrevious = false;
		for (ValidatedColumn column : addColumns) {
			if (hasPrevious) {
				sql.append(", ");
			}
			sql.append("ADD COLUMN ");
			appendColumn(sql, column);
			hasPrevious = true;
		}
		for (String column : dropColumns) {
			if (hasPrevious) {
				sql.append(", ");
			}
			sql.append("DROP COLUMN ").append(column);
			hasPrevious = true;
		}
		return sql.toString();
	}

	public String dropTable(String tableName) {
		String validatedTableName = validatedTableName(tableName);
		return "DROP TABLE IF EXISTS " + validatedTableName;
	}

	public String initializeTable(String tableName, Map<String, Object> data) {
		String validatedTableName = validatedTableName(tableName);
		List<String> dataFields = dataFields(data);
		return insertSql(validatedTableName, dataFields, "data", null);
	}

	public String describeTable(Object parameter) {
		Object rawTableName = parameter instanceof Map ? ((Map<?, ?>) parameter).get("tableName") : parameter;
		String tableName = rawTableName == null ? null : String.valueOf(rawTableName);
		validatedTableName(tableName);
		return "SELECT COLUMN_NAME AS name, DATA_TYPE AS type, IS_NULLABLE = 'YES' AS nullable "
				+ "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = #{tableName}";
	}

	public String batchInsert(String tableName, List<Map<String, Object>> rows) {
		String validatedTableName = validatedTableName(tableName);
		if (rows == null || rows.isEmpty()) {
			throw new IllegalArgumentException("Batch rows must not be empty");
		}
		List<String> fields = dataFields(rows.get(0));
		for (Map<String, Object> row : rows) {
			if (!fields.equals(dataFields(row))) {
				throw new IllegalArgumentException("Batch rows must have identical fields");
			}
		}
		return insertSql(validatedTableName, fields, "rows", Integer.valueOf(rows.size()));
	}

	private String insertSql(String tableName, List<String> fields, String parameterName, Integer rowCount) {
		StringBuilder sql = new StringBuilder("INSERT INTO ");
		sql.append(tableName).append(" (").append(join(fields, ", ")).append(") VALUES ");
		if (rowCount == null) {
			appendValueBindings(sql, fields, parameterName, null);
		} else {
			for (int rowIndex = 0; rowIndex < rowCount.intValue(); rowIndex++) {
				if (rowIndex > 0) {
					sql.append(", ");
				}
				appendValueBindings(sql, fields, parameterName, Integer.valueOf(rowIndex));
			}
		}
		return sql.toString();
	}

	private void appendValueBindings(StringBuilder sql, List<String> fields, String parameterName, Integer rowIndex) {
		sql.append('(');
		for (int i = 0; i < fields.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append("#{").append(parameterName);
			if (rowIndex != null) {
				sql.append('[').append(rowIndex.intValue()).append(']');
			}
			sql.append('.').append(fields.get(i)).append('}');
		}
		sql.append(')');
	}

	private String tableName(Map<String, Object> params) {
		if (params == null) {
			throw new IllegalArgumentException("Dynamic SQL parameters must not be null");
		}
		return validatedTableName(params.get("tableName") == null ? null : String.valueOf(params.get("tableName")));
	}

	private String validatedTableName(String tableName) {
		return TinyEngineSqlIdentifierValidator.validateTableName(tableName);
	}

	private List<String> fields(Object rawFields, boolean defaultToWildcard) {
		if (rawFields == null) {
			return defaultToWildcard ? Collections.singletonList("*") : Collections.<String>emptyList();
		}
		if (!(rawFields instanceof Collection)) {
			throw new IllegalArgumentException("Dynamic fields must be a collection");
		}
		Collection<?> fieldCollection = (Collection<?>) rawFields;
		if (fieldCollection.isEmpty()) {
			return defaultToWildcard ? Collections.singletonList("*") : Collections.<String>emptyList();
		}
		List<String> validatedFields = new ArrayList<String>(fieldCollection.size());
		for (Object field : fieldCollection) {
			validatedFields.add(TinyEngineSqlIdentifierValidator.validateFieldName(field == null ? null : String.valueOf(field)));
		}
		return validatedFields;
	}

	private List<String> conditionFields(Object rawConditions) {
		if (rawConditions == null) {
			return Collections.emptyList();
		}
		if (!(rawConditions instanceof Map)) {
			throw new IllegalArgumentException("Dynamic conditions must be a map");
		}
		List<String> fields = new ArrayList<String>();
		for (Object field : ((Map<?, ?>) rawConditions).keySet()) {
			fields.add(TinyEngineSqlIdentifierValidator.validateFieldName(field == null ? null : String.valueOf(field)));
		}
		return fields;
	}

	private List<String> dataFields(Object rawData) {
		if (!(rawData instanceof Map) || ((Map<?, ?>) rawData).isEmpty()) {
			throw new IllegalArgumentException("Dynamic data must not be empty");
		}
		List<String> fields = new ArrayList<String>();
		for (Object field : ((Map<?, ?>) rawData).keySet()) {
			fields.add(TinyEngineSqlIdentifierValidator.validateFieldName(field == null ? null : String.valueOf(field)));
		}
		return fields;
	}

	private String optionalOrderBy(Object rawOrderBy) {
		if (rawOrderBy == null || String.valueOf(rawOrderBy).trim().isEmpty()) {
			return null;
		}
		return TinyEngineSqlIdentifierValidator.validateOrderBy(String.valueOf(rawOrderBy));
	}

	private List<ValidatedColumn> validatedColumns(List<DynamicColumnDefinition> columns, String label) {
		return validatedColumns(columns, label, false);
	}

	private List<ValidatedColumn> validatedColumns(List<DynamicColumnDefinition> columns, String label, boolean allowEmpty) {
		if (columns == null || columns.isEmpty()) {
			if (allowEmpty) {
				return Collections.emptyList();
			}
			throw new IllegalArgumentException("" + label + " must contain columns");
		}
		List<ValidatedColumn> validatedColumns = new ArrayList<ValidatedColumn>(columns.size());
		for (DynamicColumnDefinition column : columns) {
			if (column == null) {
				throw new IllegalArgumentException("Column definition must not be null");
			}
			String name = TinyEngineSqlIdentifierValidator.validateFieldName(column.getName());
			String type = validatedColumnType(column.getType());
			validatedColumns.add(new ValidatedColumn(name, type, column.isNullable()));
		}
		return validatedColumns;
	}

	private String validatedColumnType(String type) {
		String normalizedType = type == null ? "" : type.toUpperCase(Locale.ROOT);
		switch (normalizedType) {
			case "VARCHAR":
				return "VARCHAR(255)";
			case "CHAR":
				return "CHAR(1)";
			case "TEXT":
			case "INTEGER":
			case "INT":
			case "BIGINT":
			case "DECIMAL":
			case "NUMERIC":
			case "DOUBLE":
			case "FLOAT":
			case "BOOLEAN":
			case "DATE":
			case "DATETIME":
			case "TIMESTAMP":
				return normalizedType;
			default:
				throw new TinyEngineSqlIdentifierException("Invalid SQL column type");
		}
	}

	private void appendColumn(StringBuilder sql, ValidatedColumn column) {
		sql.append(column.name).append(' ').append(column.type);
		if (!column.nullable) {
			sql.append(" NOT NULL");
		}
	}

	private void appendConditions(StringBuilder sql, List<String> conditions) {
		if (conditions.isEmpty()) {
			return;
		}
		sql.append(" WHERE ");
		for (int i = 0; i < conditions.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			String condition = conditions.get(i);
			sql.append(condition).append(" = #{conditions.").append(condition).append('}');
		}
	}

	private String join(List<String> values, String delimiter) {
		StringBuilder joined = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				joined.append(delimiter);
			}
			joined.append(values.get(i));
		}
		return joined.toString();
	}

	private String stringValue(Object value, String label) {
		if (value == null) {
			throw new IllegalArgumentException(label + " must not be null");
		}
		return String.valueOf(value);
	}

	private static final class ValidatedColumn {

		private final String name;
		private final String type;
		private final boolean nullable;

		private ValidatedColumn(String name, String type, boolean nullable) {
			this.name = name;
			this.type = type;
			this.nullable = nullable;
		}
	}
}
