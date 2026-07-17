package com.vesoft.nebula.jdbc.sql;

public class NebulaParameterBinding {
	private final Object value;
	private final int jdbcType;

	public NebulaParameterBinding(Object value, int jdbcType) {
		this.value = value;
		this.jdbcType = jdbcType;
	}

	public Object getValue() {
		return value;
	}

	public int getJdbcType() {
		return jdbcType;
	}
}
