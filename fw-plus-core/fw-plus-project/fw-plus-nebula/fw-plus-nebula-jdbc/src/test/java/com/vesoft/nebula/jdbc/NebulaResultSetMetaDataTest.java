package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ResultSet;
import org.junit.Test;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaResultSetMetaDataTest {

	@Test
	public void metadataShouldExposeColumnDefaultsForFrameworkMappers() throws Exception {
		ResultSetMetaData metaData = resultSet("playerName", "age").getMetaData();

		assertEquals(2, metaData.getColumnCount());
		assertEquals("playerName", metaData.getColumnLabel(1));
		assertEquals("playerName", metaData.getColumnName(1));
		assertEquals(Types.JAVA_OBJECT, metaData.getColumnType(1));
		assertEquals("JAVA_OBJECT", metaData.getColumnTypeName(1));
		assertEquals(Object.class.getName(), metaData.getColumnClassName(1));
		assertEquals(ResultSetMetaData.columnNullableUnknown, metaData.isNullable(1));
		assertTrue(metaData.isReadOnly(1));
		assertFalse(metaData.isWritable(1));
		assertFalse(metaData.isDefinitelyWritable(1));
		assertFalse(metaData.isAutoIncrement(1));
		assertFalse(metaData.isSearchable(1));
		assertFalse(metaData.isCurrency(1));
	}

	private static NebulaResultSet resultSet(String... columnNames) {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isEmpty()).thenReturn(true);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList(columnNames));
		when(nativeResultSet.getSpaceName()).thenReturn("test_space");
		return new NebulaResultSet(nativeResultSet, null);
	}
}
