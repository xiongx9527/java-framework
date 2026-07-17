package com.vesoft.nebula.jdbc.sql.query;

public class QueryColumn {
	private final String columnName;
	private final String label;
	private final int jdbcType;
	private final String source;

	public QueryColumn(String columnName, String label, int jdbcType, String source) {
		this.columnName = columnName;
		this.label = label;
		this.jdbcType = jdbcType;
		this.source = source;
	}

	public String getColumnName() {
		return columnName;
	}

	public String getLabel() {
		return label;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public String getSource() {
		return source;
	}
}
