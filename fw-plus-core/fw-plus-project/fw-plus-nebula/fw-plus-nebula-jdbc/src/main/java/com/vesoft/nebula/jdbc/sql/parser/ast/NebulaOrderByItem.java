package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaOrderByItem {

	private final NebulaExpression expression;
	private final String direction;

	public NebulaOrderByItem(NebulaExpression expression, String direction) {
		this.expression = expression;
		this.direction = direction;
	}

	public NebulaExpression getExpression() {
		return expression;
	}

	public String getDirection() {
		return direction;
	}
}
