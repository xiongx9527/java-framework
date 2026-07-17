package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaSelectItem {

	private final NebulaExpression expression;
	private final String alias;

	public NebulaSelectItem(NebulaExpression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}

	public NebulaExpression getExpression() {
		return expression;
	}

	public String getAlias() {
		return alias;
	}
}
