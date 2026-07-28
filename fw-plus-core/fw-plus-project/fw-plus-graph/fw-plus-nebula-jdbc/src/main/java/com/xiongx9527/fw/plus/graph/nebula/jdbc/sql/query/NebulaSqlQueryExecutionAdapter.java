package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaResultSet;

import java.sql.SQLException;

public class NebulaSqlQueryExecutionAdapter {
	public java.sql.ResultSet executeQuery(NebulaConnection connection, NgqlQueryPlan plan) throws SQLException {
		NebulaNativeResultSet resultSet = connection.execute(plan.getNgql());
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %s, error message: %s",
					plan.getNgql(),
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
		return new NebulaResultSet(resultSet, null);
	}
}
