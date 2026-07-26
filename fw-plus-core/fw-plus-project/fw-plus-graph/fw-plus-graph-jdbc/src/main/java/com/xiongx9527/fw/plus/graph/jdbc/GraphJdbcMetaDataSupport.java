package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphJdbcMetaDataSupport {

	private GraphJdbcMetaDataSupport() {
	}

	public static ResultSet emptySchemas() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_SCHEM", "TABLE_CATALOG"},
				new int[] {Types.VARCHAR, Types.VARCHAR});
	}

	public static ResultSet emptyCatalogs() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT"}, new int[] {Types.VARCHAR});
	}

	public static ResultSet tableTypes(String firstType, String secondType) throws SQLException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		rows.add(row("TABLE_TYPE", firstType));
		rows.add(row("TABLE_TYPE", secondType));
		return shapedResultSet(new String[] {"TABLE_TYPE"}, new int[] {Types.VARCHAR}, rows);
	}

	public static ResultSet emptyTables() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
						"TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
	}

	public static ResultSet emptyColumns() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE",
						"TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
						"NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB",
						"CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA",
						"SCOPE_TABLE", "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
						Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
						Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
						Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
						Types.VARCHAR, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR});
	}

	public static ResultSet emptyPrimaryKeys() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ",
						"PK_NAME"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.VARCHAR});
	}

	public static ResultSet emptyIndexInfo() throws SQLException {
		return emptyResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
						"INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC",
						"CARDINALITY", "PAGES", "FILTER_CONDITION"},
				new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR,
						Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR,
						Types.BIGINT, Types.BIGINT, Types.VARCHAR});
	}

	private static ResultSet emptyResultSet(String[] names, int[] types) throws SQLException {
		return shapedResultSet(names, types, Collections.<Map<String, Object>>emptyList());
	}

	private static ResultSet shapedResultSet(String[] names, int[] types, List<Map<String, Object>> rows)
			throws SQLException {
		List<GraphJdbcColumn> columns = new ArrayList<GraphJdbcColumn>();
		for (int i = 0; i < names.length; i++) {
			columns.add(GraphJdbcColumn.of(names[i], types[i]));
		}
		return GraphJdbcRowSets.shapedResultSet(columns, rows);
	}

	private static Map<String, Object> row(String name, Object value) {
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put(name, value);
		return row;
	}
}
