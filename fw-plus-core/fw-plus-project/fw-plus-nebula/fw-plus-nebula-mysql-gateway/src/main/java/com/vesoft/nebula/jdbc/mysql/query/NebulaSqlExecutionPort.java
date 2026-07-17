package com.vesoft.nebula.jdbc.mysql.query;

import java.sql.SQLException;

public interface NebulaSqlExecutionPort {
	MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) throws SQLException;
}
