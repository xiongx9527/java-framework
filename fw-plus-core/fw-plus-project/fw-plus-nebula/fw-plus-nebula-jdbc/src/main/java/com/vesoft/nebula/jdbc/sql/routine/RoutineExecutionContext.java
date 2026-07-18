package com.vesoft.nebula.jdbc.sql.routine;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;

public class RoutineExecutionContext {

	private final String schema;
	private final String user;
	private final NebulaConnection connection;
	private final NebulaSqlExecutor executor;
	private final int maxStatements;

	public RoutineExecutionContext(String schema, String user, NebulaConnection connection,
			NebulaSqlExecutor executor, int maxStatements) {
		this.schema = schema;
		this.user = user;
		this.connection = connection;
		this.executor = executor;
		this.maxStatements = maxStatements;
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
}
