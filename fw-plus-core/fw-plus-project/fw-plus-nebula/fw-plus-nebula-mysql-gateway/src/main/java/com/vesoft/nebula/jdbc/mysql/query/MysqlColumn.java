package com.vesoft.nebula.jdbc.mysql.query;

public class MysqlColumn {
	private final String label;
	private final String name;
	private final int jdbcType;
	private final boolean nullable;
	private final boolean readOnly;

	public MysqlColumn(String label, String name, int jdbcType, boolean nullable, boolean readOnly) {
		this.label = label;
		this.name = name;
		this.jdbcType = jdbcType;
		this.nullable = nullable;
		this.readOnly = readOnly;
	}

	public String getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
