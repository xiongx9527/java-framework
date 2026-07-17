package com.vesoft.nebula.jdbc;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
		assertTrue(metaData.isReadOnly());
		assertFalse(metaData.supportsTransactions());
		assertEquals(Connection.TRANSACTION_NONE, metaData.getDefaultTransactionIsolation());
		assertFalse(metaData.supportsBatchUpdates());
		assertFalse(metaData.supportsGetGeneratedKeys());
		assertFalse(metaData.supportsMultipleResultSets());
		assertFalse(metaData.supportsNamedParameters());
		assertEquals("`", metaData.getIdentifierQuoteString());
		assertTrue(metaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
		assertFalse(metaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
		assertTrue(metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
		assertFalse(metaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE));
	}
}
