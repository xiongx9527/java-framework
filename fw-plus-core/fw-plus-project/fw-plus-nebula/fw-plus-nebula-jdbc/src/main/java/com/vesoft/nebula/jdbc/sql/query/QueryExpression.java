package com.vesoft.nebula.jdbc.sql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryExpression {
	public enum Type {
		COLUMN,
		LITERAL,
		EQ,
		GTE,
		AND,
		FUNCTION
	}

	private final Type type;
	private final String tableName;
	private final String columnName;
	private final String functionName;
	private final Object literalValue;
	private final QueryExpression left;
	private final QueryExpression right;
	private final List<QueryExpression> arguments;

	private QueryExpression(Type type, String tableName, String columnName, Object literalValue,
							QueryExpression left, QueryExpression right, String functionName,
							List<QueryExpression> arguments) {
		this.type = type;
		this.tableName = tableName;
		this.columnName = columnName;
		this.functionName = functionName;
		this.literalValue = literalValue;
		this.left = left;
		this.right = right;
		this.arguments = arguments == null ? Collections.<QueryExpression>emptyList()
				: Collections.unmodifiableList(new ArrayList<QueryExpression>(arguments));
	}

	public static QueryExpression column(String tableName, String columnName) {
		return new QueryExpression(Type.COLUMN, tableName, columnName, null, null, null, null, null);
	}

	public static QueryExpression literal(Object value) {
		return new QueryExpression(Type.LITERAL, null, null, value, null, null, null, null);
	}

	public static QueryExpression eq(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.EQ, null, null, null, left, right, null, null);
	}

	public static QueryExpression gte(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.GTE, null, null, null, left, right, null, null);
	}

	public static QueryExpression and(QueryExpression left, QueryExpression right) {
		return new QueryExpression(Type.AND, null, null, null, left, right, null, null);
	}

	public static QueryExpression function(String functionName, QueryExpression argument) {
		List<QueryExpression> arguments = new ArrayList<QueryExpression>();
		arguments.add(argument);
		return function(functionName, arguments);
	}

	public static QueryExpression function(String functionName, List<QueryExpression> arguments) {
		return new QueryExpression(Type.FUNCTION, null, null, null, null, null, functionName, arguments);
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

	public String getFunctionName() {
		return functionName;
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

	public List<QueryExpression> getArguments() {
		return arguments;
	}
}
