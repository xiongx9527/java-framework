package com.vesoft.nebula.jdbc.sql.query;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TableMapping {
	private final String sqlTable;
	private final TableKind tableKind;
	private final String nebulaName;
	private final ColumnMapping idColumn;
	private final Map<String, ColumnMapping> properties;

	private TableMapping(Builder builder) {
		this.sqlTable = builder.sqlTable;
		this.tableKind = builder.tableKind;
		this.nebulaName = builder.nebulaName;
		this.idColumn = builder.idColumn;
		this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, ColumnMapping>(builder.properties));
	}

	public static Builder tag(String sqlTable, String tagName) {
		return new Builder(sqlTable, TableKind.TAG, tagName);
	}

	public String getSqlTable() {
		return sqlTable;
	}

	public TableKind getTableKind() {
		return tableKind;
	}

	public String getNebulaName() {
		return nebulaName;
	}

	public ColumnMapping getIdColumn() {
		return idColumn;
	}

	public Map<String, ColumnMapping> getProperties() {
		return properties;
	}

	public ColumnMapping getRequiredColumn(String sqlColumn) throws SQLException {
		if (idColumn != null && idColumn.getSqlColumn().equals(sqlColumn)) {
			return idColumn;
		}
		ColumnMapping columnMapping = properties.get(sqlColumn);
		if (columnMapping == null) {
			throw new SQLException("NEBULA_SQL_QUERY_MISSING_COLUMN_MAPPING: " + sqlTable + "." + sqlColumn);
		}
		return columnMapping;
	}

	public static class Builder {
		private final String sqlTable;
		private final TableKind tableKind;
		private final String nebulaName;
		private ColumnMapping idColumn;
		private final Map<String, ColumnMapping> properties = new LinkedHashMap<String, ColumnMapping>();

		private Builder(String sqlTable, TableKind tableKind, String nebulaName) {
			this.sqlTable = sqlTable;
			this.tableKind = tableKind;
			this.nebulaName = nebulaName;
		}

		public Builder idColumn(String sqlColumn, int jdbcType) {
			this.idColumn = new ColumnMapping(sqlColumn, null, ColumnRole.VID, jdbcType);
			return this;
		}

		public Builder property(String sqlColumn, String nebulaProperty, int jdbcType) {
			properties.put(sqlColumn, new ColumnMapping(sqlColumn, nebulaProperty, ColumnRole.PROPERTY, jdbcType));
			return this;
		}

		public TableMapping build() {
			return new TableMapping(this);
		}
	}
}
