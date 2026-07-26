package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphJdbcMetaDataSupportTest {

	@Test
	public void metadataShapesShouldExposeStandardColumnNames() throws Exception {
		assertColumns(GraphJdbcMetaDataSupport.emptySchemas(), "TABLE_SCHEM", "TABLE_CATALOG");
		assertColumns(GraphJdbcMetaDataSupport.emptyCatalogs(), "TABLE_CAT");
		assertColumns(GraphJdbcMetaDataSupport.emptyTables(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
				"TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION");
		assertColumns(GraphJdbcMetaDataSupport.emptyColumns(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME",
				"DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
				"NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH",
				"ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE",
				"IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN");
		assertColumns(GraphJdbcMetaDataSupport.emptyPrimaryKeys(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME",
				"COLUMN_NAME", "KEY_SEQ", "PK_NAME");
		assertColumns(GraphJdbcMetaDataSupport.emptyIndexInfo(), "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
				"INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME", "ASC_OR_DESC",
				"CARDINALITY", "PAGES", "FILTER_CONDITION");
	}

	@Test
	public void tableTypesShouldUseGraphSpecificLabels() throws Exception {
		ResultSet rs = GraphJdbcMetaDataSupport.tableTypes("NODE", "RELATIONSHIP");
		assertTrue(rs.next());
		assertEquals("NODE", rs.getString("TABLE_TYPE"));
		assertTrue(rs.next());
		assertEquals("RELATIONSHIP", rs.getString("TABLE_TYPE"));
		assertFalse(rs.next());
	}

	private void assertColumns(ResultSet rs, String... names) throws Exception {
		assertEquals(names.length, rs.getMetaData().getColumnCount());
		for (int i = 0; i < names.length; i++) {
			assertEquals(names[i], rs.getMetaData().getColumnName(i + 1));
		}
		assertFalse(rs.next());
	}
}
