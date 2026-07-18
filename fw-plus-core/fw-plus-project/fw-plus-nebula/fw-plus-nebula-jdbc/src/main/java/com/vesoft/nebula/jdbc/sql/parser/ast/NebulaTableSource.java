package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaTableSource {

	public enum Type {
		TABLE,
		JOIN,
		SUBQUERY
	}

	private final Type type;
	private final String name;
	private final String alias;
	private final NebulaTableSource left;
	private final NebulaTableSource right;
	private final String joinType;
	private final NebulaExpression condition;
	private final NebulaSelectStatement subquery;

	public NebulaTableSource(Type type, String name, String alias, NebulaTableSource left, NebulaTableSource right,
							 String joinType, NebulaExpression condition, NebulaSelectStatement subquery) {
		this.type = type;
		this.name = name;
		this.alias = alias;
		this.left = left;
		this.right = right;
		this.joinType = joinType;
		this.condition = condition;
		this.subquery = subquery;
	}

	public static NebulaTableSource table(String name, String alias) {
		return new NebulaTableSource(Type.TABLE, name, alias, null, null, null, null, null);
	}

	public static NebulaTableSource join(NebulaTableSource left, NebulaTableSource right, String joinType, NebulaExpression condition) {
		return new NebulaTableSource(Type.JOIN, null, null, left, right, joinType, condition, null);
	}

	public static NebulaTableSource subquery(NebulaSelectStatement subquery, String alias) {
		return new NebulaTableSource(Type.SUBQUERY, null, alias, null, null, null, null, subquery);
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getAlias() {
		return alias;
	}

	public NebulaTableSource getLeft() {
		return left;
	}

	public NebulaTableSource getRight() {
		return right;
	}

	public String getJoinType() {
		return joinType;
	}

	public NebulaExpression getCondition() {
		return condition;
	}

	public NebulaSelectStatement getSubquery() {
		return subquery;
	}
}
