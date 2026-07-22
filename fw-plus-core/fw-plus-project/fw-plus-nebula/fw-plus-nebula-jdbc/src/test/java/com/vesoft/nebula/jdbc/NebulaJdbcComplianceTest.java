package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.client.NebulaNativeRecord;
import com.vesoft.nebula.jdbc.client.NebulaNativeValue;
import com.vesoft.nebula.jdbc.lifecycle.NebulaResourceLifecycle;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.DirectNebulaSqlExecutor;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.statement.NebulaStatementImpl;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Properties;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.vesoft.nebula.jdbc.test.NativeResultSets.record;
import static com.vesoft.nebula.jdbc.test.NativeResultSets.rows;

public class NebulaJdbcComplianceTest {

	@Test
	public void connectionShouldImplementWrapperClientInfoAndLocalStateContracts() throws Exception {
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");

		connection.setClientInfo("applicationName", "fw-plus-test");
		Properties clientInfo = new Properties();
		clientInfo.setProperty("traceId", "trace-1");
		connection.setClientInfo(clientInfo);
		connection.setNetworkTimeout(null, 1234);
		connection.setCatalog("catalog_space");

		assertSame(connection, connection.unwrap(NebulaConnection.class));
		assertSame(connection, connection.unwrap(Connection.class));
		assertTrue(connection.isWrapperFor(NebulaConnection.class));
		assertTrue(connection.isWrapperFor(Connection.class));
		assertEquals("SELECT 1", connection.nativeSQL("SELECT 1"));
		assertEquals("trace-1", connection.getClientInfo("traceId"));
		assertEquals("trace-1", connection.getClientInfo().getProperty("traceId"));
		assertEquals(1234, connection.getNetworkTimeout());
		assertEquals("catalog_space", connection.getCatalog());
		assertEquals("catalog_space", connection.getSchema());
		assertFalse(connection.isReadOnly());
		assertTrue(connection.getTypeMap().isEmpty());
	}

	@Test
	public void connectionShouldCreateLocalLobAndSqlXmlObjects() throws Exception {
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");

		java.sql.Blob blob = connection.createBlob();
		blob.setBytes(1L, "blob".getBytes(StandardCharsets.UTF_8));
		assertArrayEquals("blob".getBytes(StandardCharsets.UTF_8), blob.getBytes(1L, 4));

		java.sql.Clob clob = connection.createClob();
		clob.setString(1L, "clob");
		assertEquals("clob", clob.getSubString(1L, 4));

		NClob nclob = connection.createNClob();
		nclob.setString(1L, "中文");
		assertEquals("中文", nclob.getSubString(1L, 2));

		SQLXML sqlxml = connection.createSQLXML();
		sqlxml.setString("<root/>");
		assertEquals("<root/>", sqlxml.getString());

		Struct struct = connection.createStruct("player_identity", new Object[] {"player100", Integer.valueOf(30)});
		assertEquals("player_identity", struct.getSQLTypeName());
		assertArrayEquals(new Object[] {"player100", Integer.valueOf(30)}, struct.getAttributes());
	}

	@Test
	public void statementShouldImplementSupportedLocalStateAndBatchContracts() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(2, 3);
		Statement statement = new NebulaStatementImpl(mock(NebulaConnection.class), executor);

		statement.setMaxRows(100);
		statement.setFetchSize(20);
		statement.setFetchDirection(ResultSet.FETCH_REVERSE);
		statement.setQueryTimeout(5);
		statement.setPoolable(true);
		statement.closeOnCompletion();
		statement.addBatch("INSERT VERTEX player(name) VALUES \"p1\":(\"Tim\")");
		statement.addBatch("INSERT VERTEX player(name) VALUES \"p2\":(\"Tony\")");

		assertSame(statement, statement.unwrap(Statement.class));
		assertTrue(statement.isWrapperFor(NebulaStatementImpl.class));
		assertEquals(100, statement.getMaxRows());
		assertEquals(20, statement.getFetchSize());
		assertEquals(ResultSet.FETCH_REVERSE, statement.getFetchDirection());
		assertEquals(5, statement.getQueryTimeout());
		assertTrue(statement.isPoolable());
		assertTrue(statement.isCloseOnCompletion());
		assertArrayEquals(new int[] {2, 3}, statement.executeBatch());
		assertEquals(Arrays.asList(
				"INSERT VERTEX player(name) VALUES \"p1\":(\"Tim\")",
				"INSERT VERTEX player(name) VALUES \"p2\":(\"Tony\")"
		), executor.sqls);
		statement.clearWarnings();
	}

	@Test
	public void preparedStatementShouldImplementCommonStandardParameterContracts() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(1);
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"INSERT VERTEX payload(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10) VALUES ?:(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				executor
		);
		byte[] bytes = new byte[] {1, 2, 3};
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");
		java.sql.Array array = connection.createArrayOf("VARCHAR", new Object[] {"a", "b"});

		statement.setLong(1, 1001L);
		statement.setBytes(2, bytes);
		statement.setNString(3, "中文");
		statement.setObject(4, Integer.valueOf(7), JDBCType.INTEGER);
		statement.setObject(5, "value", JDBCType.VARCHAR, 10);
		statement.setAsciiStream(6, new ByteArrayInputStream("ascii".getBytes(StandardCharsets.US_ASCII)), 5);
		statement.setCharacterStream(7, new StringReader("reader"), 6);
		statement.setNCharacterStream(8, new StringReader("汉字"), 2L);
		statement.setBlob(9, new SerialBlob("blob".getBytes(StandardCharsets.UTF_8)));
		statement.setClob(10, new SerialClob("clob".toCharArray()));
		statement.setArray(11, array);
		statement.addBatch();

		ParameterMetaData metaData = statement.getParameterMetaData();
		assertEquals("VARBINARY", metaData.getParameterTypeName(2));
		assertFalse(metaData.isSigned(2));
		assertEquals(3, metaData.getPrecision(2));
		assertEquals("NVARCHAR", metaData.getParameterTypeName(3));

		assertArrayEquals(new int[] {1}, statement.executeBatch());
		assertSame(bytes, executor.batchBindings.get(0).get(2).getValue());
		assertEquals(Types.VARBINARY, executor.batchBindings.get(0).get(2).getJdbcType());
		assertEquals("中文", executor.batchBindings.get(0).get(3).getValue());
		assertEquals(Types.NVARCHAR, executor.batchBindings.get(0).get(3).getJdbcType());
		assertEquals(Integer.valueOf(7), executor.batchBindings.get(0).get(4).getValue());
		assertEquals(Types.INTEGER, executor.batchBindings.get(0).get(4).getJdbcType());
		assertEquals("value", executor.batchBindings.get(0).get(5).getValue());
		assertEquals(Types.VARCHAR, executor.batchBindings.get(0).get(5).getJdbcType());
		assertEquals(Integer.valueOf(10), executor.batchBindings.get(0).get(5).getScaleOrLength());
		assertEquals("ascii", executor.batchBindings.get(0).get(6).getValue());
		assertEquals(Types.LONGVARCHAR, executor.batchBindings.get(0).get(6).getJdbcType());
		assertEquals(Integer.valueOf(5), executor.batchBindings.get(0).get(6).getScaleOrLength());
		assertEquals("reader", executor.batchBindings.get(0).get(7).getValue());
		assertEquals(Types.LONGVARCHAR, executor.batchBindings.get(0).get(7).getJdbcType());
		assertEquals(Integer.valueOf(6), executor.batchBindings.get(0).get(7).getScaleOrLength());
		assertEquals("汉字", executor.batchBindings.get(0).get(8).getValue());
		assertEquals(Types.LONGNVARCHAR, executor.batchBindings.get(0).get(8).getJdbcType());
		assertEquals(Integer.valueOf(2), executor.batchBindings.get(0).get(8).getScaleOrLength());
		assertArrayEquals("blob".getBytes(StandardCharsets.UTF_8), (byte[]) executor.batchBindings.get(0).get(9).getValue());
		assertEquals(Types.BLOB, executor.batchBindings.get(0).get(9).getJdbcType());
		assertEquals("clob", executor.batchBindings.get(0).get(10).getValue());
		assertEquals(Types.CLOB, executor.batchBindings.get(0).get(10).getJdbcType());
		assertArrayEquals(new Object[] {"a", "b"}, (Object[]) executor.batchBindings.get(0).get(11).getValue());
		assertEquals(Types.ARRAY, executor.batchBindings.get(0).get(11).getJdbcType());
		assertEquals("VARCHAR", array.getBaseTypeName());
		assertArrayEquals(new Object[] {"a", "b"}, (Object[]) array.getArray());
	}

	@Test
	public void preparedStatementShouldBindNClobSqlXmlAndUnicodeStreamContracts() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(1);
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"INSERT VERTEX payload(v1, v2, v3) VALUES ?:(?, ?, ?)",
				executor
		);
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");
		NClob nclob = connection.createNClob();
		nclob.setString(1L, "nclob");
		SQLXML sqlxml = connection.createSQLXML();
		sqlxml.setString("<root/>");

		statement.setLong(1, 1001L);
		statement.setNClob(2, nclob);
		statement.setSQLXML(3, sqlxml);
		statement.setUnicodeStream(4, new ByteArrayInputStream("unicode".getBytes(StandardCharsets.UTF_8)), 7);
		statement.addBatch();

		assertArrayEquals(new int[] {1}, statement.executeBatch());
		assertEquals("nclob", executor.batchBindings.get(0).get(2).getValue());
		assertEquals(Types.NCLOB, executor.batchBindings.get(0).get(2).getJdbcType());
		assertEquals("<root/>", executor.batchBindings.get(0).get(3).getValue());
		assertEquals(Types.SQLXML, executor.batchBindings.get(0).get(3).getJdbcType());
		assertEquals("unicode", executor.batchBindings.get(0).get(4).getValue());
		assertEquals(Types.LONGNVARCHAR, executor.batchBindings.get(0).get(4).getJdbcType());
	}

	@Test
	public void preparedStatementShouldBindRowIdRefAndStructAsStandardJdbcObjects() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(1);
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"INSERT VERTEX payload(v1, v2, v3) VALUES ?:(?, ?, ?)",
				executor
		);
		RowId rowId = rowId("player100");
		Ref ref = ref("player_ref", "player200");
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");
		Struct struct = connection.createStruct("player_struct", new Object[] {"player300"});

		statement.setLong(1, 1001L);
		statement.setRowId(2, rowId);
		statement.setRef(3, ref);
		statement.setObject(4, struct);
		statement.addBatch();

		assertArrayEquals(new int[] {1}, statement.executeBatch());
		assertEquals("player100", executor.batchBindings.get(0).get(2).getValue());
		assertEquals(Types.ROWID, executor.batchBindings.get(0).get(2).getJdbcType());
		assertEquals("player200", executor.batchBindings.get(0).get(3).getValue());
		assertEquals(Types.REF, executor.batchBindings.get(0).get(3).getJdbcType());
		assertSame(struct, executor.batchBindings.get(0).get(4).getValue());
		assertEquals(Types.STRUCT, executor.batchBindings.get(0).get(4).getJdbcType());
	}

	@Test
	public void sqlXmlShouldSupportDomSaxAndStaxSourcesAndResults() throws Exception {
		SQLXML sqlxml = new NebulaSQLXML("<root><name>Tim</name></root>");

		DOMSource domSource = sqlxml.getSource(DOMSource.class);
		assertEquals("root", domSource.getNode().getFirstChild().getNodeName());
		SAXSource saxSource = sqlxml.getSource(SAXSource.class);
		assertTrue(saxSource.getInputSource().getCharacterStream() != null);
		StAXSource staxSource = sqlxml.getSource(StAXSource.class);
		assertTrue(staxSource.getXMLStreamReader().hasNext());

		SQLXML domXml = new NebulaSQLXML();
		DOMResult domResult = domXml.setResult(DOMResult.class);
		domResult.setNode(DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader("<dom/>"))));
		assertTrue(domXml.getString().contains("<dom"));

		SQLXML saxXml = new NebulaSQLXML();
		SAXResult saxResult = saxXml.setResult(SAXResult.class);
		saxResult.getHandler().startDocument();
		saxResult.getHandler().startElement("", "sax", "sax", new AttributesImpl());
		saxResult.getHandler().endElement("", "sax", "sax");
		saxResult.getHandler().endDocument();
		assertTrue(saxXml.getString().contains("<sax"));

		SQLXML staxXml = new NebulaSQLXML();
		StAXResult staxResult = staxXml.setResult(StAXResult.class);
		XMLStreamWriter writer = staxResult.getXMLStreamWriter();
		writer.writeStartDocument();
		writer.writeEmptyElement("stax");
		writer.writeEndDocument();
		writer.close();
		assertTrue(staxXml.getString().contains("<stax"));
	}

	@Test
	public void statementMaxRowsShouldLimitReturnedResultSetRows() throws Exception {
		ResultSet resultSet = new NebulaResultSet(nativeResultSetWithRows(
				Collections.singletonList("name"),
				new String[][] {{"a"}, {"b"}, {"c"}}), null);
		QueryExecutor executor = new QueryExecutor(resultSet);
		Statement statement = new NebulaStatementImpl(mock(NebulaConnection.class), executor);

		statement.setMaxRows(2);
		ResultSet limited = statement.executeQuery("YIELD 1 AS name");

		assertTrue(limited.next());
		assertEquals("a", limited.getString(1));
		assertTrue(limited.next());
		assertEquals("b", limited.getString(1));
		assertFalse(limited.next());
	}

	@Test
	public void preparedStatementMaxRowsShouldLimitReturnedResultSetRows() throws Exception {
		ResultSet resultSet = new NebulaResultSet(nativeResultSetWithRows(
				Collections.singletonList("name"),
				new String[][] {{"a"}, {"b"}, {"c"}}), null);
		QueryExecutor executor = new QueryExecutor(resultSet);
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"YIELD ? AS name",
				executor);

		statement.setMaxRows(1);
		statement.setString(1, "ignored");
		ResultSet limited = statement.executeQuery();

		assertTrue(limited.next());
		assertEquals("a", limited.getString(1));
		assertFalse(limited.next());
	}

	@Test
	public void resultSetShouldImplementTypedObjectAndWrapperContracts() throws Exception {
		ResultSet resultSet = nativeResultSetWithSingleString("age", "42");

		assertTrue(resultSet.next());
		assertEquals(Integer.valueOf(42), resultSet.getObject(1, Integer.class));
		assertEquals(Long.valueOf(42L), resultSet.getObject("age", Long.class));
		assertEquals("42", resultSet.getObject("age", String.class));
		assertSame(resultSet, resultSet.unwrap(ResultSet.class));
		assertTrue(resultSet.isWrapperFor(NebulaResultSet.class));
	}

	@Test
	public void resultSetShouldImplementCommonNumericAndFetchContracts() throws Exception {
		ResultSet resultSet = nativeResultSetWithSingleString("value", "42");

		assertTrue(resultSet.next());
		assertEquals((byte) 42, resultSet.getByte(1));
		assertEquals((short) 42, resultSet.getShort("value"));
		assertEquals(42.0f, resultSet.getFloat(1), 0.001f);
		assertEquals(new java.math.BigDecimal("42"), resultSet.getBigDecimal("value"));
		assertArrayEquals("42".getBytes(StandardCharsets.UTF_8), resultSet.getBytes(1));
		assertEquals("42", readAll(resultSet.getCharacterStream("value")));
		assertEquals("42", resultSet.getNString(1));
		assertEquals("42", readAll(resultSet.getNCharacterStream("value")));
		ResultSet urlResultSet = nativeResultSetWithSingleString("value", "http://127.0.0.1:7001");
		assertTrue(urlResultSet.next());
		assertEquals(new URL("http://127.0.0.1:7001"), urlResultSet.getObject(1, URL.class));

		resultSet.setFetchDirection(ResultSet.FETCH_REVERSE);
		resultSet.setFetchSize(64);
		assertEquals(ResultSet.FETCH_REVERSE, resultSet.getFetchDirection());
		assertEquals(64, resultSet.getFetchSize());
		assertEquals(null, resultSet.getCursorName());
		assertFalse(resultSet.rowUpdated());
		assertFalse(resultSet.rowInserted());
		assertFalse(resultSet.rowDeleted());
	}

	@Test
	public void resultSetNumericConversionFailuresShouldThrowSqlException() throws Exception {
		ResultSet resultSet = nativeResultSetWithSingleString("value", "not-number");

		assertTrue(resultSet.next());
		try {
			resultSet.getBigDecimal("value");
			fail("Expected SQLException");
		} catch (java.sql.SQLException exception) {
			assertTrue(exception.getMessage().contains("Cannot convert"));
		}
	}

	@Test
	public void resultSetShouldReadLocalLobAndSqlXmlConvenienceValues() throws Exception {
		ResultSet blobResultSet = nativeResultSetWithSingleString("payload", "blob");
		assertTrue(blobResultSet.next());
		assertArrayEquals("blob".getBytes(StandardCharsets.UTF_8), blobResultSet.getBlob("payload").getBytes(1L, 4));

		ResultSet clobResultSet = nativeResultSetWithSingleString("payload", "clob");
		assertTrue(clobResultSet.next());
		assertEquals("clob", clobResultSet.getClob(1).getSubString(1L, 4));

		ResultSet nclobResultSet = nativeResultSetWithSingleString("payload", "中文");
		assertTrue(nclobResultSet.next());
		assertEquals("中文", nclobResultSet.getNClob("payload").getSubString(1L, 2));

		ResultSet sqlxmlResultSet = nativeResultSetWithSingleString("payload", "<root/>");
		assertTrue(sqlxmlResultSet.next());
		assertEquals("<root/>", sqlxmlResultSet.getSQLXML(1).getString());

		ResultSet rowIdResultSet = nativeResultSetWithSingleString("id", "player100");
		assertTrue(rowIdResultSet.next());
		assertArrayEquals("player100".getBytes(StandardCharsets.UTF_8), rowIdResultSet.getRowId("id").getBytes());

		ResultSet refResultSet = nativeResultSetWithSingleString("ref", "player101");
		assertTrue(refResultSet.next());
		assertEquals("JAVA_OBJECT", refResultSet.getRef(1).getBaseTypeName());
		assertEquals("player101", refResultSet.getRef(1).getObject());
	}

	@Test
	public void resultSetShouldImplementCalendarAndArrayReadContracts() throws Exception {
		ResultSet resultSet = nativeResultSetWithSingleList("items", Arrays.asList("a", "b"));

		assertTrue(resultSet.next());
		java.sql.Array array = resultSet.getArray("items");
		assertEquals("JAVA_OBJECT", array.getBaseTypeName());
		assertArrayEquals(new Object[] {"a", "b"}, (Object[]) array.getArray());
		ResultSet arrayRows = array.getResultSet();
		assertTrue(arrayRows.next());
		assertEquals(1L, arrayRows.getLong(1));
		assertEquals("a", arrayRows.getString(2));
		assertTrue(arrayRows.next());
		assertEquals(2L, arrayRows.getLong("INDEX"));
		assertEquals("b", arrayRows.getString("VALUE"));
		assertFalse(arrayRows.next());
		ResultSet arraySliceRows = array.getResultSet(2L, 1);
		assertTrue(arraySliceRows.next());
		assertEquals(2L, arraySliceRows.getLong("INDEX"));
		assertEquals("b", arraySliceRows.getString("VALUE"));
		assertFalse(arraySliceRows.next());

		ResultSet objectWithMapResultSet = nativeResultSetWithSingleString("item", "mapped");
		assertTrue(objectWithMapResultSet.next());
		assertEquals("mapped", objectWithMapResultSet.getObject(1, Collections.<String, Class<?>>emptyMap()));

		ResultSet dateResultSet = nativeResultSetWithSingleDate("created");
		Calendar calendar = Calendar.getInstance();
		assertTrue(dateResultSet.next());
		assertEquals(dateResultSet.getDate(1), dateResultSet.getDate(1, calendar));
		assertEquals(dateResultSet.getTime(1), dateResultSet.getTime("created", calendar));
		assertEquals(dateResultSet.getTimestamp(1), dateResultSet.getTimestamp("created", calendar));
	}

	@Test
	public void directExecutorShouldRenderByteArrayParametersAsStableStringLiterals() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		com.vesoft.nebula.jdbc.client.NebulaNativeResultSet nativeResultSet =
				mock(com.vesoft.nebula.jdbc.client.NebulaNativeResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		when(connection.execute("MATCH (v) WHERE v.payload == \"AQID\" RETURN v")).thenReturn(nativeResultSet);
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				connection,
				"MATCH (v) WHERE v.payload == ? RETURN v",
				new DirectNebulaSqlExecutor()
		);

		statement.setBytes(1, new byte[] {1, 2, 3});
		statement.executeQuery();

		verify(connection).execute("MATCH (v) WHERE v.payload == \"AQID\" RETURN v");
	}

	@Test
	public void databaseMetaDataShouldAnswerFrameworkProbeMethodsWithoutUnsupportedExceptions() throws Exception {
		NebulaConnection connection = realMethodConnection("jdbc:nebula://127.0.0.1:9669/test_space", "test_space");
		doReturn(nativeResultSetWithRows(Collections.singletonList("Name"), new String[0][]))
				.when(connection).execute("SHOW TAGS");
		doReturn(nativeResultSetWithRows(Collections.singletonList("Name"), new String[0][]))
				.when(connection).execute("SHOW EDGES");
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(connection);

		assertSame(metaData, metaData.unwrap(DatabaseMetaData.class));
		assertTrue(metaData.isWrapperFor(NebulaDatabaseMetaData.class));
		assertTrue(metaData.allTablesAreSelectable());
		assertFalse(metaData.allProceduresAreCallable());
		assertTrue(metaData.supportsBatchUpdates());
		assertEquals("graphspace", metaData.getSchemaTerm());
		assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, metaData.getResultSetHoldability());
		assertTrue(metaData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		assertEquals(4, metaData.getJDBCMajorVersion());
		assertEquals(2, metaData.getJDBCMinorVersion());

		ResultSet schemas = metaData.getSchemas();
		assertTrue(schemas.next());
		assertEquals("test_space", schemas.getString("TABLE_SCHEM"));
		assertFalse(schemas.next());

		ResultSet tables = metaData.getTables(null, null, null, null);
		assertFalse(tables.next());
		assertEquals("TABLE_NAME", tables.getMetaData().getColumnLabel(3));

		ResultSet tableTypes = metaData.getTableTypes();
		assertTrue(tableTypes.next());
		assertEquals("TAG", tableTypes.getString("TABLE_TYPE"));
		assertTrue(tableTypes.next());
		assertEquals("EDGE", tableTypes.getString("TABLE_TYPE"));
	}

	@Test
	public void unsupportedUnwrapShouldThrowSqlException() throws Exception {
		Statement statement = new NebulaStatementImpl(mock(NebulaConnection.class));

		try {
			statement.unwrap(PreparedStatement.class);
			fail("Expected SQLException");
		} catch (java.sql.SQLException exception) {
			assertTrue(exception.getMessage().contains("Cannot unwrap"));
		}
	}

	private static NebulaConnection realMethodConnection(String url, String graphspace) throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class, CALLS_REAL_METHODS);
		Properties properties = new Properties();
		properties.setProperty(NebulaPropertyKey.DBNAME.getKeyName(), graphspace);
		setField(connection, "properties", properties);
		setField(connection, "url", url);
		setField(connection, "graphSpace", graphspace);
		setField(connection, "lifecycle", new NebulaResourceLifecycle());
		return connection;
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = NebulaConnection.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static ResultSet nativeResultSetWithSingleString(String columnName, String value) throws Exception {
		return new NebulaResultSet(nativeResultSetWithRows(Collections.singletonList(columnName), new String[][] {{value}}), null);
	}

	private static com.vesoft.nebula.jdbc.client.NebulaNativeResultSet nativeResultSetWithRows(
			java.util.List<String> columnNames, String[][] rows) throws Exception {
		return rows(columnNames, rows);
	}

	private static ResultSet nativeResultSetWithSingleList(String columnName, java.util.List<String> value) throws Exception {
		com.vesoft.nebula.jdbc.client.NebulaNativeResultSet nativeResultSet =
				mock(com.vesoft.nebula.jdbc.client.NebulaNativeResultSet.class);
		NebulaNativeRecord record = mock(NebulaNativeRecord.class);
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		ArrayList<NebulaNativeValue> values = new ArrayList<NebulaNativeValue>();
		for (String item : value) {
			NebulaNativeValue itemWrapper = mock(NebulaNativeValue.class);
			when(itemWrapper.isNull()).thenReturn(false);
			when(itemWrapper.asString()).thenReturn(item);
			values.add(itemWrapper);
		}
		when(valueWrapper.isNull()).thenReturn(false);
		when(valueWrapper.asList()).thenReturn(values);
		record = record(Collections.singletonList(columnName), Collections.singletonList(valueWrapper));
		nativeResultSet = rows(Collections.singletonList(columnName), Collections.singletonList(record));
		return new NebulaResultSet(nativeResultSet, null);
	}

	private static ResultSet nativeResultSetWithSingleDate(String columnName) throws Exception {
		com.vesoft.nebula.jdbc.client.NebulaNativeResultSet nativeResultSet =
				mock(com.vesoft.nebula.jdbc.client.NebulaNativeResultSet.class);
		NebulaNativeRecord record = mock(NebulaNativeRecord.class);
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isNull()).thenReturn(false);
		when(valueWrapper.isDate()).thenReturn(true);
		when(valueWrapper.isTime()).thenReturn(true);
		when(valueWrapper.isDateTime()).thenReturn(true);
		when(valueWrapper.asDate()).thenReturn(java.sql.Date.valueOf("2026-07-22"));
		when(valueWrapper.asTime()).thenReturn(java.sql.Time.valueOf("08:09:10"));
		when(valueWrapper.asTimestamp()).thenReturn(java.sql.Timestamp.valueOf("2026-07-22 08:09:10"));
		record = record(Collections.singletonList(columnName), Collections.singletonList(valueWrapper));
		nativeResultSet = rows(Collections.singletonList(columnName), Collections.singletonList(record));
		return new NebulaResultSet(nativeResultSet, null);
	}

	private static String readAll(Reader reader) throws Exception {
		StringBuilder builder = new StringBuilder();
		char[] buffer = new char[16];
		int count;
		while ((count = reader.read(buffer)) >= 0) {
			builder.append(buffer, 0, count);
		}
		return builder.toString();
	}

	private static RowId rowId(final String value) {
		return new RowId() {
			@Override
			public byte[] getBytes() {
				return value.getBytes(StandardCharsets.UTF_8);
			}
		};
	}

	private static Ref ref(final String typeName, final Object value) {
		return new Ref() {
			private Object currentValue = value;

			@Override
			public String getBaseTypeName() {
				return typeName;
			}

			@Override
			public Object getObject(java.util.Map<String, Class<?>> map) {
				return currentValue;
			}

			@Override
			public Object getObject() {
				return currentValue;
			}

			@Override
			public void setObject(Object value) {
				currentValue = value;
			}
		};
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final int[] updateCounts;
		private int updateIndex;
		private final java.util.List<String> sqls = new java.util.ArrayList<String>();
		private final java.util.List<NebulaParameterBindings> batchBindings = new java.util.ArrayList<NebulaParameterBindings>();

		private RecordingExecutor(int... updateCounts) {
			this.updateCounts = updateCounts;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			throw new AssertionError("not used");
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws java.sql.SQLException {
			sqls.add(sql);
			batchBindings.add(bindings.copy());
			return updateCounts[updateIndex++];
		}
	}

	private static class QueryExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet;

		private QueryExecutor(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			throw new AssertionError("not used");
		}
	}
}
