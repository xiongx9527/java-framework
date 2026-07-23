package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutor;

public class RoutineExecutionContext {

	private final String schema;
	private final String user;
	private final NebulaConnection connection;
	private final NebulaSqlExecutor executor;
	private final int maxStatements;
	private final long maxExecutionMillis;
	private final long startedAtMillis;

	public RoutineExecutionContext(String schema, String user, NebulaConnection connection,
			NebulaSqlExecutor executor, int maxStatements, long maxExecutionMillis) {
		this.schema = schema;
		this.user = user;
		this.connection = connection;
		this.executor = executor;
		this.maxStatements = maxStatements;
		this.maxExecutionMillis = maxExecutionMillis;
		this.startedAtMillis = System.currentTimeMillis();
	}

	public String getSchema() {
		return schema;
	}

	public String getUser() {
		return user;
	}

	public NebulaConnection getConnection() {
		return connection;
	}

	public NebulaSqlExecutor getExecutor() {
		return executor;
	}

	public int getMaxStatements() {
		return maxStatements;
	}

	public void checkTimeout() throws RoutineException {
		if (maxExecutionMillis <= 0) {
			throw RoutineException.executionLimit("Routine execution timed out");
		}
		if (System.currentTimeMillis() - startedAtMillis >= maxExecutionMillis) {
			throw RoutineException.executionLimit("Routine execution timed out");
		}
	}
}
