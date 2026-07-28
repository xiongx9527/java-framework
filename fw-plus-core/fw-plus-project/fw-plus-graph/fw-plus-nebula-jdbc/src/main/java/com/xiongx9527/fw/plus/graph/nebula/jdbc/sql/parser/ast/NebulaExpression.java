package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaExpression {

	public enum Type {
		COLUMN,
		LITERAL,
		PARAMETER,
		BINARY,
		FUNCTION,
		WILDCARD,
		IN_LIST,
		LIKE,
		IS_NULL,
		UNARY
	}

	private final Type type;
	private final String name;
	private final String operator;
	private final Object value;
	private final Integer parameterIndex;
	private final NebulaExpression left;
	private final NebulaExpression right;
	private final List<NebulaExpression> arguments;
	private final boolean aggregate;

	public NebulaExpression(Type type, String name, String operator, Object value, Integer parameterIndex,
							NebulaExpression left, NebulaExpression right, List<NebulaExpression> arguments,
							boolean aggregate) {
		this.type = type;
		this.name = name;
		this.operator = operator;
		this.value = value;
		this.parameterIndex = parameterIndex;
		this.left = left;
		this.right = right;
		this.arguments = arguments == null ? Collections.<NebulaExpression>emptyList()
				: Collections.unmodifiableList(new ArrayList<NebulaExpression>(arguments));
		this.aggregate = aggregate;
	}

	public static NebulaExpression column(String name) {
		return new NebulaExpression(Type.COLUMN, name, null, null, null, null, null, null, false);
	}

	public static NebulaExpression literal(Object value) {
		return new NebulaExpression(Type.LITERAL, null, null, value, null, null, null, null, false);
	}

	public static NebulaExpression parameter(int index) {
		return new NebulaExpression(Type.PARAMETER, null, null, null, index, null, null, null, false);
	}

	public static NebulaExpression binary(String operator, NebulaExpression left, NebulaExpression right) {
		return new NebulaExpression(Type.BINARY, null, operator, null, null, left, right, null, false);
	}

	public static NebulaExpression function(String name, List<NebulaExpression> arguments, boolean aggregate) {
		return new NebulaExpression(Type.FUNCTION, name, null, null, null, null, null, arguments, aggregate);
	}

	public static NebulaExpression wildcard() {
		return new NebulaExpression(Type.WILDCARD, "*", null, null, null, null, null, null, false);
	}

	public static NebulaExpression listExpression(Type type, String operator, NebulaExpression left, List<NebulaExpression> arguments) {
		return new NebulaExpression(type, null, operator, null, null, left, null, arguments, false);
	}

	public static NebulaExpression unary(String operator, NebulaExpression expression) {
		return new NebulaExpression(Type.UNARY, null, operator, null, null, expression, null, null, false);
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getOperator() {
		return operator;
	}

	public Object getValue() {
		return value;
	}

	public Integer getParameterIndex() {
		return parameterIndex;
	}

	public NebulaExpression getLeft() {
		return left;
	}

	public NebulaExpression getRight() {
		return right;
	}

	public List<NebulaExpression> getArguments() {
		return arguments;
	}

	public boolean isAggregate() {
		return aggregate;
	}
}
