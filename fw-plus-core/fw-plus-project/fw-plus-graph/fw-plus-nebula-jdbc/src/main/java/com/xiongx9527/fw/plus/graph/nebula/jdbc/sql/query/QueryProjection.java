package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

public class QueryProjection {
	private final QueryExpression expression;
	private final String alias;

	public QueryProjection(QueryExpression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}

	public QueryExpression getExpression() {
		return expression;
	}

	public String getAlias() {
		return alias;
	}
}
