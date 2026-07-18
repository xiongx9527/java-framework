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
	private final ColumnMapping srcColumn;
	private final ColumnMapping dstColumn;
	private final ColumnMapping rankColumn;
	private final Map<String, ColumnMapping> properties;

	private TableMapping(Builder builder) {
		this.sqlTable = builder.sqlTable;
		this.tableKind = builder.tableKind;
		this.nebulaName = builder.nebulaName;
		this.idColumn = builder.idColumn;
		this.srcColumn = builder.srcColumn;
		this.dstColumn = builder.dstColumn;
		this.rankColumn = builder.rankColumn;
		this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, ColumnMapping>(builder.properties));
	}

	public static Builder tag(String sqlTable, String tagName) {
		return new Builder(sqlTable, TableKind.TAG, tagName);
	}

	public static Builder edge(String sqlTable, String edgeName) {
		return new Builder(sqlTable, TableKind.EDGE, edgeName);
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

	public ColumnMapping getSrcColumn() {
		return srcColumn;
	}

	public ColumnMapping getDstColumn() {
		return dstColumn;
	}

	public ColumnMapping getRankColumn() {
		return rankColumn;
	}

	public Map<String, ColumnMapping> getProperties() {
		return properties;
	}

	public ColumnMapping getRequiredColumn(String sqlColumn) throws SQLException {
		if (idColumn != null && idColumn.getSqlColumn().equals(sqlColumn)) {
			return idColumn;
		}
		if (srcColumn != null && srcColumn.getSqlColumn().equals(sqlColumn)) {
			return srcColumn;
		}
		if (dstColumn != null && dstColumn.getSqlColumn().equals(sqlColumn)) {
			return dstColumn;
		}
		if (rankColumn != null && rankColumn.getSqlColumn().equals(sqlColumn)) {
			return rankColumn;
		}
		ColumnMapping columnMapping = properties.get(sqlColumn);
		if (columnMapping == null) {
			throw NebulaQueryConversionException.of(NebulaQueryErrorCode.MISSING_COLUMN_MAPPING, sqlTable + "." + sqlColumn);
		}
		return columnMapping;
	}

	public static class Builder {
		private final String sqlTable;
		private final TableKind tableKind;
		private final String nebulaName;
		private ColumnMapping idColumn;
		private ColumnMapping srcColumn;
		private ColumnMapping dstColumn;
		private ColumnMapping rankColumn;
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

		public Builder srcColumn(String sqlColumn, int jdbcType) {
			this.srcColumn = new ColumnMapping(sqlColumn, null, ColumnRole.SRC_ID, jdbcType);
			return this;
		}

		public Builder dstColumn(String sqlColumn, int jdbcType) {
			this.dstColumn = new ColumnMapping(sqlColumn, null, ColumnRole.DST_ID, jdbcType);
			return this;
		}

		public Builder rankColumn(String sqlColumn, int jdbcType) {
			this.rankColumn = new ColumnMapping(sqlColumn, null, ColumnRole.RANK, jdbcType);
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
