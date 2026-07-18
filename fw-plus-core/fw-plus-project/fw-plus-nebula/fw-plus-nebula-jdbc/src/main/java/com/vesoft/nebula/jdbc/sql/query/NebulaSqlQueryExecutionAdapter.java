package com.vesoft.nebula.jdbc.sql.query;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;

import java.sql.SQLException;

public class NebulaSqlQueryExecutionAdapter {
	public java.sql.ResultSet executeQuery(NebulaConnection connection, NgqlQueryPlan plan) throws SQLException {
		ResultSet resultSet = connection.execute(plan.getNgql());
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %d, error message: %s",
					plan.getNgql(),
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
		return new NebulaResultSet(resultSet, null);
	}
}
