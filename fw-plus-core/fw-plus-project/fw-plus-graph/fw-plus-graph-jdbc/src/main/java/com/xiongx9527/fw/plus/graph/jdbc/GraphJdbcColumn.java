package com.xiongx9527.fw.plus.graph.jdbc;

public final class GraphJdbcColumn {

	private final String name;
	private final String label;
	private final int jdbcType;

	private GraphJdbcColumn(String name, String label, int jdbcType) {
		this.name = name;
		this.label = label;
		this.jdbcType = jdbcType;
	}

	public static GraphJdbcColumn of(String name, int jdbcType) {
		return new GraphJdbcColumn(name, name, jdbcType);
	}

	public static GraphJdbcColumn of(String name, String label, int jdbcType) {
		return new GraphJdbcColumn(name, label, jdbcType);
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public int getJdbcType() {
		return jdbcType;
	}
}
