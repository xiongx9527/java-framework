package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.NebulaConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface NebulaSqlExecutor {
	ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;

	int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException;
}
