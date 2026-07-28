package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.xiongx9527.fw.plus.graph.nebula.jdbc.test.NativeResultSets.rows;

public class NebulaDatabaseMetaDataTest {

	@Test
	public void metadataShouldExposeStableFrameworkProbeValues() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		Properties properties = new Properties();
		properties.setProperty("user", "nebula");
		when(connection.getUrl()).thenReturn("jdbc:nebula://127.0.0.1:9669/test_space");
		when(connection.getConnectionConfig()).thenReturn(properties);
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		assertSame(connection, metaData.getConnection());
		assertEquals("jdbc:nebula://127.0.0.1:9669/test_space", metaData.getURL());
		assertEquals("nebula", metaData.getUserName());
		assertEquals("NebulaGraph", metaData.getDatabaseProductName());
		assertEquals("", metaData.getDatabaseProductVersion());
		assertEquals("Nebula JDBC Driver", metaData.getDriverName());
		assertEquals("0.1.1-SNAPSHOT", metaData.getDriverVersion());
		assertEquals(1, metaData.getDriverMajorVersion());
		assertEquals(0, metaData.getDriverMinorVersion());
		assertFalse(metaData.isReadOnly());
		assertFalse(metaData.supportsTransactions());
		assertEquals(Connection.TRANSACTION_NONE, metaData.getDefaultTransactionIsolation());
		assertTrue(metaData.supportsBatchUpdates());
		assertFalse(metaData.supportsGetGeneratedKeys());
		assertFalse(metaData.supportsMultipleResultSets());
		assertFalse(metaData.supportsNamedParameters());
		assertTrue(metaData.supportsStoredProcedures());
		assertEquals("`", metaData.getIdentifierQuoteString());
		assertTrue(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
		assertFalse(metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
		assertTrue(metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
		assertFalse(metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE));
	}

	@Test
	public void metadataShouldReadTablesFromNebulaSchemaCommands() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.getSchema()).thenReturn("basketballplayer");
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tags = rows(
				Collections.singletonList("Name"), new String[][] {{"player"}, {"team"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tagsAgain = rows(
				Collections.singletonList("Name"), new String[][] {{"player"}, {"team"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edges = rows(
				Collections.singletonList("Name"), new String[][] {{"serve"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edgesAgain = rows(
				Collections.singletonList("Name"), new String[][] {{"serve"}});
		when(connection.execute("SHOW TAGS")).thenReturn(tags, tagsAgain);
		when(connection.execute("SHOW EDGES")).thenReturn(edges, edgesAgain);
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		ResultSet tables = metaData.getTables(null, "basketballplayer", "%", null);

		assertTrue(tables.next());
		assertEquals("player", tables.getString("TABLE_NAME"));
		assertEquals("TAG", tables.getString("TABLE_TYPE"));
		assertTrue(tables.next());
		assertEquals("team", tables.getString("TABLE_NAME"));
		assertEquals("TAG", tables.getString("TABLE_TYPE"));
		assertTrue(tables.next());
		assertEquals("serve", tables.getString("TABLE_NAME"));
		assertEquals("EDGE", tables.getString("TABLE_TYPE"));
		assertFalse(tables.next());

		ResultSet filtered = metaData.getTables(null, null, "pla%", new String[] {"TAG"});
		assertTrue(filtered.next());
		assertEquals("player", filtered.getString("TABLE_NAME"));
		assertFalse(filtered.next());
	}

	@Test
	public void metadataShouldReadColumnsFromNebulaDescribeCommands() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.getSchema()).thenReturn("basketballplayer");
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tags = rows(
				Collections.singletonList("Name"),
				new String[][] {{"player"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edges = rows(
				Collections.singletonList("Name"),
				new String[][] {{"serve"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet playerColumns = rows(
				Arrays.asList("Field", "Type", "Null", "Default", "Comment"),
				new String[][] {{"name", "string", "YES", "", ""}, {"age", "int64", "YES", "", ""}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet serveColumns = rows(
				Arrays.asList("Field", "Type", "Null", "Default", "Comment"),
				new String[][] {{"start_year", "int64", "YES", "", ""}});
		when(connection.execute("SHOW TAGS")).thenReturn(tags);
		when(connection.execute("SHOW EDGES")).thenReturn(edges);
		when(connection.execute("DESCRIBE TAG `player`")).thenReturn(playerColumns);
		when(connection.execute("DESCRIBE EDGE `serve`")).thenReturn(serveColumns);
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		ResultSet columns = metaData.getColumns(null, null, "player", "%");

		assertTrue(columns.next());
		assertEquals("player", columns.getString("TABLE_NAME"));
		assertEquals("name", columns.getString("COLUMN_NAME"));
		assertEquals(Types.VARCHAR, columns.getInt("DATA_TYPE"));
		assertEquals("string", columns.getString("TYPE_NAME"));
		assertEquals(1, columns.getInt("ORDINAL_POSITION"));
		assertTrue(columns.next());
		assertEquals("age", columns.getString("COLUMN_NAME"));
		assertEquals(Types.BIGINT, columns.getInt("DATA_TYPE"));
		assertEquals(2, columns.getInt("ORDINAL_POSITION"));
		assertFalse(columns.next());
	}

	@Test
	public void metadataShouldExposeNebulaIdentityColumnsAsPseudoPrimaryKeys() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.getSchema()).thenReturn("basketballplayer");
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tags = rows(
				Collections.singletonList("Name"), new String[][] {{"player"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tagsAgain = rows(
				Collections.singletonList("Name"), new String[][] {{"player"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edges = rows(
				Collections.singletonList("Name"), new String[][] {{"serve"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edgesAgain = rows(
				Collections.singletonList("Name"), new String[][] {{"serve"}});
		when(connection.execute("SHOW TAGS")).thenReturn(tags, tagsAgain);
		when(connection.execute("SHOW EDGES")).thenReturn(edges, edgesAgain);
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		ResultSet tagKeys = metaData.getPrimaryKeys(null, null, "player");
		assertTrue(tagKeys.next());
		assertEquals("basketballplayer", tagKeys.getString("TABLE_SCHEM"));
		assertEquals("player", tagKeys.getString("TABLE_NAME"));
		assertEquals("__vid", tagKeys.getString("COLUMN_NAME"));
		assertEquals(1, tagKeys.getShort("KEY_SEQ"));
		assertEquals("NEBULA_VERTEX_ID", tagKeys.getString("PK_NAME"));
		assertFalse(tagKeys.next());

		ResultSet edgeKeys = metaData.getPrimaryKeys(null, null, "serve");
		assertTrue(edgeKeys.next());
		assertEquals("__src", edgeKeys.getString("COLUMN_NAME"));
		assertEquals(1, edgeKeys.getShort("KEY_SEQ"));
		assertTrue(edgeKeys.next());
		assertEquals("__dst", edgeKeys.getString("COLUMN_NAME"));
		assertEquals(2, edgeKeys.getShort("KEY_SEQ"));
		assertTrue(edgeKeys.next());
		assertEquals("__rank", edgeKeys.getString("COLUMN_NAME"));
		assertEquals(3, edgeKeys.getShort("KEY_SEQ"));
		assertFalse(edgeKeys.next());
	}

	@Test
	public void metadataShouldReadIndexInfoFromNebulaIndexCommands() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.getSchema()).thenReturn("basketballplayer");
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet tagIndexes = rows(
				Arrays.asList("Index Name", "By Tag", "Columns"),
				new String[][] {{"player_age_idx", "player", "[\"age\"]"}, {"player_name_idx", "player", "[\"name\"]"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet edgeIndexes = rows(
				Arrays.asList("Index Name", "By Edge", "Columns"),
				new String[][] {{"serve_start_idx", "serve", "[\"start_year\"]"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet playerAgeIndex = rows(
				Arrays.asList("Field", "Type"),
				new String[][] {{"age", "int64"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet playerNameIndex = rows(
				Arrays.asList("Field", "Type"),
				new String[][] {{"name", "fixed_string(16)"}});
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet serveStartIndex = rows(
				Arrays.asList("Field", "Type"),
				new String[][] {{"start_year", "int64"}});
		when(connection.execute("SHOW TAG INDEXES")).thenReturn(tagIndexes);
		when(connection.execute("SHOW EDGE INDEXES")).thenReturn(edgeIndexes);
		when(connection.execute("DESCRIBE TAG INDEX `player_age_idx`")).thenReturn(playerAgeIndex);
		when(connection.execute("DESCRIBE TAG INDEX `player_name_idx`")).thenReturn(playerNameIndex);
		when(connection.execute("DESCRIBE EDGE INDEX `serve_start_idx`")).thenReturn(serveStartIndex);
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		ResultSet indexes = metaData.getIndexInfo(null, null, "player", false, false);

		assertTrue(indexes.next());
		assertEquals("player", indexes.getString("TABLE_NAME"));
		assertEquals(true, indexes.getBoolean("NON_UNIQUE"));
		assertEquals("player_age_idx", indexes.getString("INDEX_NAME"));
		assertEquals(DatabaseMetaData.tableIndexOther, indexes.getShort("TYPE"));
		assertEquals(1, indexes.getShort("ORDINAL_POSITION"));
		assertEquals("age", indexes.getString("COLUMN_NAME"));
		assertTrue(indexes.next());
		assertEquals("player_name_idx", indexes.getString("INDEX_NAME"));
		assertEquals("name", indexes.getString("COLUMN_NAME"));
		assertFalse(indexes.next());

		ResultSet uniqueIndexes = metaData.getIndexInfo(null, null, "player", true, false);
		assertFalse(uniqueIndexes.next());
	}
}
