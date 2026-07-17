package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import org.junit.Test;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

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

	@Test
	public void metadataShouldInferJdbcTypesFromNativeValues() throws Exception {
		ResultSet nativeResultSet = mock(ResultSet.class);
		ValueWrapper name = mock(ValueWrapper.class);
		ValueWrapper age = mock(ValueWrapper.class);
		ValueWrapper active = mock(ValueWrapper.class);
		ValueWrapper score = mock(ValueWrapper.class);
		when(nativeResultSet.isEmpty()).thenReturn(false);
		when(nativeResultSet.rowsSize()).thenReturn(1);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList("name", "age", "active", "score"));
		when(nativeResultSet.getSpaceName()).thenReturn("test_space");
		when(nativeResultSet.colValues("name")).thenReturn(Collections.singletonList(name));
		when(nativeResultSet.colValues("age")).thenReturn(Collections.singletonList(age));
		when(nativeResultSet.colValues("active")).thenReturn(Collections.singletonList(active));
		when(nativeResultSet.colValues("score")).thenReturn(Collections.singletonList(score));
		when(name.isString()).thenReturn(true);
		when(age.isLong()).thenReturn(true);
		when(active.isBoolean()).thenReturn(true);
		when(score.isDouble()).thenReturn(true);

		ResultSetMetaData metaData = new NebulaResultSet(nativeResultSet, null).getMetaData();

		assertEquals(Types.VARCHAR, metaData.getColumnType(1));
		assertEquals("VARCHAR", metaData.getColumnTypeName(1));
		assertEquals(String.class.getName(), metaData.getColumnClassName(1));
		assertEquals(Types.BIGINT, metaData.getColumnType(2));
		assertEquals("BIGINT", metaData.getColumnTypeName(2));
		assertEquals(Long.class.getName(), metaData.getColumnClassName(2));
		assertEquals(Types.BOOLEAN, metaData.getColumnType(3));
		assertEquals(Boolean.class.getName(), metaData.getColumnClassName(3));
		assertEquals(Types.DOUBLE, metaData.getColumnType(4));
		assertEquals(Double.class.getName(), metaData.getColumnClassName(4));
		assertTrue(metaData.isSigned(2));
		assertTrue(metaData.isSigned(4));
	}

	private static NebulaResultSet resultSet(String... columnNames) {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isEmpty()).thenReturn(true);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList(columnNames));
		when(nativeResultSet.getSpaceName()).thenReturn("test_space");
		return new NebulaResultSet(nativeResultSet, null);
	}
}
