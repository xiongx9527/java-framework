package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConflictTarget {
	public enum Type {
		NONE,
		COLUMNS,
		CONSTRAINT,
		EXPRESSION
	}

	private static final ConflictTarget NONE = new ConflictTarget(Type.NONE,
			Collections.<String>emptyList(), null, null);

	private final Type type;
	private final List<String> columns;
	private final String constraintName;
	private final Expression expression;

	private ConflictTarget(Type type, List<String> columns, String constraintName, Expression expression) {
		this.type = type;
		this.columns = Collections.unmodifiableList(new ArrayList<String>(
				columns == null ? Collections.<String>emptyList() : columns));
		this.constraintName = constraintName;
		this.expression = expression;
	}

	public static ConflictTarget none() { return NONE; }

	public static ConflictTarget columns(List<String> columns) {
		return new ConflictTarget(Type.COLUMNS, columns, null, null);
	}

	public static ConflictTarget constraint(String constraintName) {
		return new ConflictTarget(Type.CONSTRAINT, Collections.<String>emptyList(), constraintName, null);
	}

	public static ConflictTarget expression(Expression expression) {
		return new ConflictTarget(Type.EXPRESSION, Collections.<String>emptyList(), null, expression);
	}

	public Type getType() { return type; }
	public List<String> getColumns() { return columns; }
	public String getConstraintName() { return constraintName; }
	public Expression getExpression() { return expression; }
}
