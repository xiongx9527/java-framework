package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaColumnDefinition {

	private final String name;
	private final String dataType;
	private final boolean primaryKey;

	public NebulaColumnDefinition(String name, String dataType, boolean primaryKey) {
		this.name = name;
		this.dataType = dataType;
		this.primaryKey = primaryKey;
	}

	public String getName() {
		return name;
	}

	public String getDataType() {
		return dataType;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}
}
