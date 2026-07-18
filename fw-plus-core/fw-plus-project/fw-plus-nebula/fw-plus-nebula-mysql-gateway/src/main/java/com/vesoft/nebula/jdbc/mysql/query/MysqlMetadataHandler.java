package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;

public interface MysqlMetadataHandler {
	MysqlQueryResult showDatabases(MysqlProtocolSession session) throws SQLException;

	MysqlQueryResult showTables(MysqlProtocolSession session) throws SQLException;

	MysqlQueryResult describe(MysqlProtocolSession session, String tableName) throws SQLException;
}
