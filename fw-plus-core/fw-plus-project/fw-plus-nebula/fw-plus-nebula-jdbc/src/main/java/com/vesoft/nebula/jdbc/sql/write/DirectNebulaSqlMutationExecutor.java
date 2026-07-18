package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;

import java.sql.SQLException;

public class DirectNebulaSqlMutationExecutor implements NebulaSqlMutationExecutor {
	private final NebulaConnection connection;

	public DirectNebulaSqlMutationExecutor(NebulaConnection connection) {
		this.connection = connection;
	}

	public NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException {
		executeNative(plan.getNgql());
		return new NebulaMutationResult(true, 1, plan.getDescriptor(), null,
				null, String.valueOf(System.nanoTime()));
	}

	public NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException {
		executeNative(plan.getNgql());
		return new NebulaMutationResult(true, 0, null, plan.getDescriptor(),
				plan.getWarnings(), String.valueOf(System.nanoTime()));
	}

	private void executeNative(String ngql) throws SQLException {
		ResultSet resultSet = connection.execute(ngql);
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %d, error message: %s",
					ngql,
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
	}
}
