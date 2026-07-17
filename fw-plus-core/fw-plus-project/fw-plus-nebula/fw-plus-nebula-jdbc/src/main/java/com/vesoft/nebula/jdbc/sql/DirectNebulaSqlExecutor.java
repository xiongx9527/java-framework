package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DirectNebulaSqlExecutor implements NebulaSqlExecutor {
	@Override
	public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkQuery(sql);
		return new NebulaResultSet(executeNative(connection, sql), null);
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkUpdate(sql);
		executeNative(connection, sql);
		return 0;
	}

	private com.vesoft.nebula.client.graph.data.ResultSet executeNative(NebulaConnection connection, String sql)
			throws SQLException {
		com.vesoft.nebula.client.graph.data.ResultSet resultSet = connection.execute(sql);
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %d, error message: %s",
					sql,
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
		return resultSet;
	}
}
