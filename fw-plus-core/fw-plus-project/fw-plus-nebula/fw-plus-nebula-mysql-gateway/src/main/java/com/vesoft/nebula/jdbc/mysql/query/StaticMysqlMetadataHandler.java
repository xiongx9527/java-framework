package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class StaticMysqlMetadataHandler implements MysqlMetadataHandler {
	private final String database;
	private final Map<String, List<MysqlColumn>> tables;

	public StaticMysqlMetadataHandler(String database) {
		this(database, Collections.<String, List<MysqlColumn>>emptyMap());
	}

	public StaticMysqlMetadataHandler(String database, Map<String, List<MysqlColumn>> tables) {
		this.database = database;
		this.tables = new HashMap<String, List<MysqlColumn>>();
		if (tables != null) {
			for (Map.Entry<String, List<MysqlColumn>> entry : tables.entrySet()) {
				this.tables.put(entry.getKey().toLowerCase(java.util.Locale.ENGLISH), new ArrayList<MysqlColumn>(entry.getValue()));
			}
		}
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
		List<List<Object>> rows = new ArrayList<List<Object>>();
		for (String tableName : tables.keySet()) {
			rows.add(Collections.<Object>singletonList(tableName));
		}
		return MysqlQueryResult.of(
				Collections.singletonList(new MysqlColumn("Tables_in_" + session.getDatabase(), "Tables_in_" + session.getDatabase(), Types.VARCHAR, false, true)),
				rows
		);
	}

	@Override
	public MysqlQueryResult describe(MysqlProtocolSession session, String tableName) throws SQLException {
		String normalized = tableName == null ? "" : tableName.toLowerCase(java.util.Locale.ENGLISH);
		List<MysqlColumn> tableColumns = tables.get(normalized);
		if (tableColumns == null) {
			throw MysqlErrorMapper.tableNotFound(tableName);
		}
		List<List<Object>> rows = new ArrayList<List<Object>>();
		for (MysqlColumn column : tableColumns) {
			rows.add(Arrays.<Object>asList(
					column.getName(),
					toMysqlTypeName(column.getJdbcType()),
					column.isNullable() ? "YES" : "NO",
					"",
					null,
					""
			));
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
				rows
		);
	}

	@Override
	public MysqlQueryResult showVariables(MysqlProtocolSession session) {
		List<List<Object>> rows = new ArrayList<List<Object>>();
		rows.add(Arrays.<Object>asList("version", "5.7.0-nebula-gateway"));
		rows.add(Arrays.<Object>asList("version_comment", "Nebula MySQL protocol gateway"));
		rows.add(Arrays.<Object>asList("character_set_client", "utf8mb4"));
		rows.add(Arrays.<Object>asList("character_set_connection", "utf8mb4"));
		return MysqlQueryResult.of(
				Arrays.asList(
						new MysqlColumn("Variable_name", "Variable_name", Types.VARCHAR, false, true),
						new MysqlColumn("Value", "Value", Types.VARCHAR, true, true)
				),
				rows
		);
	}

	@Override
	public MysqlQueryResult selectDatabase(MysqlProtocolSession session) {
		return MysqlQueryResult.of(
				Collections.singletonList(new MysqlColumn("DATABASE()", "DATABASE()", Types.VARCHAR, true, true)),
				Collections.<List<Object>>singletonList(Collections.<Object>singletonList(session.getDatabase()))
		);
	}

	@Override
	public MysqlQueryResult informationSchemaTables(MysqlProtocolSession session) {
		List<List<Object>> rows = new ArrayList<List<Object>>();
		for (String tableName : tables.keySet()) {
			rows.add(Arrays.<Object>asList("def", session.getDatabase(), tableName, "BASE TABLE"));
		}
		return MysqlQueryResult.of(
				Arrays.asList(
						new MysqlColumn("TABLE_CATALOG", "TABLE_CATALOG", Types.VARCHAR, false, true),
						new MysqlColumn("TABLE_SCHEMA", "TABLE_SCHEMA", Types.VARCHAR, false, true),
						new MysqlColumn("TABLE_NAME", "TABLE_NAME", Types.VARCHAR, false, true),
						new MysqlColumn("TABLE_TYPE", "TABLE_TYPE", Types.VARCHAR, false, true)
				),
				rows
		);
	}

	@Override
	public MysqlQueryResult informationSchemaColumns(MysqlProtocolSession session, String tableName) throws SQLException {
		List<List<Object>> rows = new ArrayList<List<Object>>();
		if (tableName == null || tableName.trim().length() == 0) {
			for (String currentTable : tables.keySet()) {
				addColumnRows(session, rows, currentTable, tables.get(currentTable));
			}
		} else {
			String normalized = tableName.toLowerCase(java.util.Locale.ENGLISH);
			List<MysqlColumn> tableColumns = tables.get(normalized);
			if (tableColumns == null) {
				throw MysqlErrorMapper.tableNotFound(tableName);
			}
			addColumnRows(session, rows, normalized, tableColumns);
		}
		return MysqlQueryResult.of(
				Arrays.asList(
						new MysqlColumn("TABLE_CATALOG", "TABLE_CATALOG", Types.VARCHAR, false, true),
						new MysqlColumn("TABLE_SCHEMA", "TABLE_SCHEMA", Types.VARCHAR, false, true),
						new MysqlColumn("TABLE_NAME", "TABLE_NAME", Types.VARCHAR, false, true),
						new MysqlColumn("COLUMN_NAME", "COLUMN_NAME", Types.VARCHAR, false, true),
						new MysqlColumn("ORDINAL_POSITION", "ORDINAL_POSITION", Types.INTEGER, false, true),
						new MysqlColumn("COLUMN_DEFAULT", "COLUMN_DEFAULT", Types.VARCHAR, true, true),
						new MysqlColumn("IS_NULLABLE", "IS_NULLABLE", Types.VARCHAR, false, true),
						new MysqlColumn("DATA_TYPE", "DATA_TYPE", Types.VARCHAR, false, true)
				),
				rows
		);
	}

	private void addColumnRows(MysqlProtocolSession session, List<List<Object>> rows,
			String tableName, List<MysqlColumn> tableColumns) {
		for (int i = 0; i < tableColumns.size(); i++) {
			MysqlColumn column = tableColumns.get(i);
			rows.add(Arrays.<Object>asList(
					"def",
					session.getDatabase(),
					tableName,
					column.getName(),
					i + 1,
					null,
					column.isNullable() ? "YES" : "NO",
					toMysqlTypeName(column.getJdbcType())
			));
		}
	}

	private String toMysqlTypeName(int jdbcType) {
		if (jdbcType == Types.INTEGER || jdbcType == Types.SMALLINT || jdbcType == Types.TINYINT) {
			return "int";
		}
		if (jdbcType == Types.BIGINT) {
			return "bigint";
		}
		if (jdbcType == Types.DOUBLE || jdbcType == Types.FLOAT || jdbcType == Types.REAL) {
			return "double";
		}
		if (jdbcType == Types.BOOLEAN || jdbcType == Types.BIT) {
			return "tinyint";
		}
		if (jdbcType == Types.TIMESTAMP || jdbcType == Types.DATE || jdbcType == Types.TIME) {
			return "datetime";
		}
		return "varchar";
	}
}
