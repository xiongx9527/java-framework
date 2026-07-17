package com.vesoft.nebula.jdbc.sql.query;

public class QueryExpression {
	public enum Type {
		COLUMN,
		LITERAL,
		EQ,
		GTE,
		AND
	}

	private final Type type;
	private final String tableName;
	private final String columnName;
	private final Object literalValue;
	private final QueryExpression left;
	private final QueryExpression right;

	private QueryExpression(Type type, String tableName, String columnName, Object literalValue,
							QueryExpression left, QueryExpression right) {
		this.type = type;
		this.tableName = tableName;
		this.columnName = columnName;
		this.literalValue = literalValue;
		this.left = left;
		this.right = right;
	}

	public static QueryExpression column(String tableName, String columnName) {
		return new QueryExpression(Type.COLUMN, tableName, columnName, null, null, null);
	}

	public static QueryExpression literal(Object value) {
		return new QueryExpression(Type.LITERAL, null, null, value, null, null);
	}

	public static QueryExpression eq(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.EQ, null, null, null, left, right);
	}

	public static QueryExpression gte(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.GTE, null, null, null, left, right);
	}

	public static QueryExpression and(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.AND, null, null, null, left, right);
	}

	public Type getType() {
		return type;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public Object getLiteralValue() {
		return literalValue;
	}

	public QueryExpression getLeft() {
		return left;
	}

	public QueryExpression getRight() {
		return right;
	}
}
