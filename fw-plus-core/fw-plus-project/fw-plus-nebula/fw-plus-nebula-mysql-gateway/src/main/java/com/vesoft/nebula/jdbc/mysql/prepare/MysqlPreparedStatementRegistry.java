package com.vesoft.nebula.jdbc.mysql.prepare;

import java.util.HashMap;
import java.util.Map;

public class MysqlPreparedStatementRegistry {
	private final Map<Long, Map<Integer, MysqlPreparedStatement>> statements =
			new HashMap<Long, Map<Integer, MysqlPreparedStatement>>();
	private int nextStatementId = 1;

	public MysqlPreparedStatement add(long connectionId, String sql, int parameterCount) {
		Map<Integer, MysqlPreparedStatement> sessionStatements = statements.get(connectionId);
		if (sessionStatements == null) {
			sessionStatements = new HashMap<Integer, MysqlPreparedStatement>();
			statements.put(connectionId, sessionStatements);
		}
		MysqlPreparedStatement statement = new MysqlPreparedStatement(connectionId, nextStatementId++, sql, parameterCount);
		sessionStatements.put(statement.getStatementId(), statement);
		return statement;
	}

	public MysqlPreparedStatement get(long connectionId, int statementId) {
		Map<Integer, MysqlPreparedStatement> sessionStatements = statements.get(connectionId);
		return sessionStatements == null ? null : sessionStatements.get(statementId);
	}

	public boolean contains(long connectionId, int statementId) {
		return get(connectionId, statementId) != null;
	}

	public void remove(long connectionId, int statementId) {
		Map<Integer, MysqlPreparedStatement> sessionStatements = statements.get(connectionId);
		if (sessionStatements != null) {
			sessionStatements.remove(statementId);
		}
	}
}
