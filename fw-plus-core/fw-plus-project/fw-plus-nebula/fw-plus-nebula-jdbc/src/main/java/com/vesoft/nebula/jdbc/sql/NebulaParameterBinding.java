package com.vesoft.nebula.jdbc.sql;

public class NebulaParameterBinding {
	private final Object value;
	private final int jdbcType;
	private final Integer scaleOrLength;

	public NebulaParameterBinding(Object value, int jdbcType) {
		this(value, jdbcType, null);
	}

	public NebulaParameterBinding(Object value, int jdbcType, Integer scaleOrLength) {
		this.value = value;
		this.jdbcType = jdbcType;
		this.scaleOrLength = scaleOrLength;
	}

	public Object getValue() {
		return value;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public Integer getScaleOrLength() {
		return scaleOrLength;
	}
}
