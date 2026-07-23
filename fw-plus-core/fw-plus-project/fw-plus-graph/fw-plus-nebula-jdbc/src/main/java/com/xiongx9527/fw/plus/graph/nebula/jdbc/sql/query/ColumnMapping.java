package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

public class ColumnMapping {
	private final String sqlColumn;
	private final String nebulaProperty;
	private final ColumnRole role;
	private final int jdbcType;

	public ColumnMapping(String sqlColumn, String nebulaProperty, ColumnRole role, int jdbcType) {
		this.sqlColumn = sqlColumn;
		this.nebulaProperty = nebulaProperty;
		this.role = role;
		this.jdbcType = jdbcType;
	}

	public String getSqlColumn() {
		return sqlColumn;
	}

	public String getNebulaProperty() {
		return nebulaProperty;
	}

	public ColumnRole getRole() {
		return role;
	}

	public int getJdbcType() {
		return jdbcType;
	}
}
