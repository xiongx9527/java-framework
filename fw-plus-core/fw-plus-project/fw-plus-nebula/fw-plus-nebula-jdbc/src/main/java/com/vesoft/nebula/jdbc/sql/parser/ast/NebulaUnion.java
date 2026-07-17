package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaUnion {

	private final NebulaSelectStatement left;
	private final NebulaSelectStatement right;
	private final String operator;

	public NebulaUnion(NebulaSelectStatement left, NebulaSelectStatement right, String operator) {
		this.left = left;
		this.right = right;
		this.operator = operator;
	}

	public NebulaSelectStatement getLeft() {
		return left;
	}

	public NebulaSelectStatement getRight() {
		return right;
	}

	public String getOperator() {
		return operator;
	}
}
