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
		return new NebulaResultSet(connection.execute(sql), null);
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkUpdate(sql);
		connection.execute(sql);
		return 0;
	}
}
