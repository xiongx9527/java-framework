package com.vesoft.nebula.jdbc.mysql.prepare;

public class MysqlPreparedStatement {
	private final long connectionId;
	private final int statementId;
	private final String sql;
	private final int parameterCount;

	public MysqlPreparedStatement(long connectionId, int statementId, String sql, int parameterCount) {
		this.connectionId = connectionId;
		this.statementId = statementId;
		this.sql = sql;
		this.parameterCount = parameterCount;
	}

	public long getConnectionId() {
		return connectionId;
	}

	public int getStatementId() {
		return statementId;
	}

	public String getSql() {
		return sql;
	}

	public int getParameterCount() {
		return parameterCount;
	}
}
