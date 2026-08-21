package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast;

public class NebulaUpdateItem {

	private final String column;
	private final NebulaExpression value;

	public NebulaUpdateItem(String column, NebulaExpression value) {
		this.column = column;
		this.value = value;
	}

	public String getColumn() {
		return column;
	}

	public NebulaExpression getValue() {
		return value;
	}
}
