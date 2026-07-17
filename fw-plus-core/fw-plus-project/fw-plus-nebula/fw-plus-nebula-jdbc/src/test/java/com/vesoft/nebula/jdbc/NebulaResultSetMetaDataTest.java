package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import org.junit.Test;

import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
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

	@Test
	public void temporalMetadataShouldMatchResultSetReadMethods() throws Exception {
		ResultSet nativeResultSet = mock(ResultSet.class);
		ResultSet.Record record = mock(ResultSet.Record.class);
		ValueWrapper birthDate = mock(ValueWrapper.class);
		ValueWrapper alarmTime = mock(ValueWrapper.class);
		ValueWrapper createdAt = mock(ValueWrapper.class);
		when(nativeResultSet.isEmpty()).thenReturn(false);
		when(nativeResultSet.rowsSize()).thenReturn(1);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList("birthDate", "alarmTime", "createdAt"));
		when(nativeResultSet.getSpaceName()).thenReturn("test_space");
		when(nativeResultSet.colValues("birthDate")).thenReturn(Collections.singletonList(birthDate));
		when(nativeResultSet.colValues("alarmTime")).thenReturn(Collections.singletonList(alarmTime));
		when(nativeResultSet.colValues("createdAt")).thenReturn(Collections.singletonList(createdAt));
		when(nativeResultSet.rowValues(0)).thenReturn(record);
		when(record.get(0)).thenReturn(birthDate);
		when(record.get(1)).thenReturn(alarmTime);
		when(record.get(2)).thenReturn(createdAt);
		when(birthDate.isDate()).thenReturn(true);
		when(birthDate.asDate()).thenReturn(new com.vesoft.nebula.client.graph.data.DateWrapper(
				new com.vesoft.nebula.Date((short) 2026, (byte) 7, (byte) 17)
		));
		when(alarmTime.isTime()).thenReturn(true);
		when(alarmTime.asTime()).thenReturn(new com.vesoft.nebula.client.graph.data.TimeWrapper(
				new com.vesoft.nebula.Time((byte) 8, (byte) 9, (byte) 10, 123000)
		));
		when(createdAt.isDateTime()).thenReturn(true);
		when(createdAt.asDateTime()).thenReturn(new com.vesoft.nebula.client.graph.data.DateTimeWrapper(
				new com.vesoft.nebula.DateTime((short) 2026, (byte) 7, (byte) 17, (byte) 8, (byte) 9, (byte) 10, 123000)
		));

		NebulaResultSet resultSet = new NebulaResultSet(nativeResultSet, null);
		resultSet.next();
		ResultSetMetaData metaData = resultSet.getMetaData();

		assertEquals(Types.DATE, metaData.getColumnType(1));
		assertEquals(Date.class.getName(), metaData.getColumnClassName(1));
		assertEquals(Date.valueOf("2026-07-17"), resultSet.getObject(1));
		assertEquals(Types.TIME, metaData.getColumnType(2));
		assertEquals(Time.class.getName(), metaData.getColumnClassName(2));
		assertEquals(Time.valueOf("08:09:10"), resultSet.getObject(2));
		assertEquals(Types.TIMESTAMP, metaData.getColumnType(3));
		assertEquals(Timestamp.class.getName(), metaData.getColumnClassName(3));
		assertEquals(Timestamp.valueOf("2026-07-17 08:09:10.123"), resultSet.getObject(3));
		assertEquals(Timestamp.valueOf("2026-07-17 08:09:10.123"), resultSet.getTimestamp("createdAt"));
	}

	private static NebulaResultSet resultSet(String... columnNames) {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isEmpty()).thenReturn(true);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList(columnNames));
		when(nativeResultSet.getSpaceName()).thenReturn("test_space");
		return new NebulaResultSet(nativeResultSet, null);
	}
}
