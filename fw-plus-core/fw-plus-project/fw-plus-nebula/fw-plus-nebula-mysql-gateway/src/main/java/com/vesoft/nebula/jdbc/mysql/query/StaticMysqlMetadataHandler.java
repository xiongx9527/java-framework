package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

public class StaticMysqlMetadataHandler implements MysqlMetadataHandler {
	private final String database;

	public StaticMysqlMetadataHandler(String database) {
		this.database = database;
	}

	@Override
	public MysqlQueryResult showDatabases(MysqlProtocolSession session) {
		return MysqlQueryResult.of(
				Collections.singletonList(new MysqlColumn("Database", "Database", Types.VARCHAR, false, true)),
				Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList(database))
		);
	}

	@Override
	public MysqlQueryResult showTables(MysqlProtocolSession session) {
		return MysqlQueryResult.of(
				Collections.singletonList(new MysqlColumn("Tables_in_" + session.getDatabase(), "Tables_in_" + session.getDatabase(), Types.VARCHAR, false, true)),
				Collections.<java.util.List<Object>>emptyList()
		);
	}

	@Override
	public MysqlQueryResult describe(MysqlProtocolSession session, String tableName) throws SQLException {
		if (tableName == null || tableName.length() == 0) {
			throw MysqlErrorMapper.tableNotFound("");
		}
		return MysqlQueryResult.of(
				Arrays.asList(
						new MysqlColumn("Field", "Field", Types.VARCHAR, false, true),
						new MysqlColumn("Type", "Type", Types.VARCHAR, false, true),
						new MysqlColumn("Null", "Null", Types.VARCHAR, false, true),
						new MysqlColumn("Key", "Key", Types.VARCHAR, true, true),
						new MysqlColumn("Default", "Default", Types.VARCHAR, true, true),
						new MysqlColumn("Extra", "Extra", Types.VARCHAR, true, true)
				),
				Collections.<java.util.List<Object>>emptyList()
		);
	}
}
