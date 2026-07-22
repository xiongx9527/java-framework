package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.client.NebulaNativeResultSet;
import com.vesoft.nebula.jdbc.client.NebulaNativeRecord;
import com.vesoft.nebula.jdbc.client.NebulaNativeValue;
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
import static com.vesoft.nebula.jdbc.test.NativeResultSets.record;
import static com.vesoft.nebula.jdbc.test.NativeResultSets.rows;

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
		NebulaNativeResultSet nativeResultSet = mock(NebulaNativeResultSet.class);
		NebulaNativeValue name = mock(NebulaNativeValue.class);
		NebulaNativeValue age = mock(NebulaNativeValue.class);
		NebulaNativeValue active = mock(NebulaNativeValue.class);
		NebulaNativeValue score = mock(NebulaNativeValue.class);
		when(name.isString()).thenReturn(true);
		when(age.isLong()).thenReturn(true);
		when(active.isBoolean()).thenReturn(true);
		when(score.isDouble()).thenReturn(true);
		nativeResultSet = rows(
				Arrays.asList("name", "age", "active", "score"),
				Collections.singletonList(record(Arrays.asList("name", "age", "active", "score"),
						Arrays.asList(name, age, active, score))));

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
		NebulaNativeResultSet nativeResultSet = mock(NebulaNativeResultSet.class);
		NebulaNativeRecord record = mock(NebulaNativeRecord.class);
		NebulaNativeValue birthDate = mock(NebulaNativeValue.class);
		NebulaNativeValue alarmTime = mock(NebulaNativeValue.class);
		NebulaNativeValue createdAt = mock(NebulaNativeValue.class);
		when(birthDate.isDate()).thenReturn(true);
		when(birthDate.asDate()).thenReturn(Date.valueOf("2026-07-17"));
		when(alarmTime.isTime()).thenReturn(true);
		when(alarmTime.asTime()).thenReturn(Time.valueOf("08:09:10"));
		when(createdAt.isDateTime()).thenReturn(true);
		when(createdAt.asTimestamp()).thenReturn(Timestamp.valueOf("2026-07-17 08:09:10.123"));
		record = record(Arrays.asList("birthDate", "alarmTime", "createdAt"),
				Arrays.asList(birthDate, alarmTime, createdAt));
		nativeResultSet = rows(Arrays.asList("birthDate", "alarmTime", "createdAt"),
				Collections.singletonList(record));

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

	private static NebulaResultSet resultSet(String... columnNames) throws Exception {
		NebulaNativeResultSet nativeResultSet = mock(NebulaNativeResultSet.class);
		when(nativeResultSet.isEmpty()).thenReturn(true);
		when(nativeResultSet.getColumnNames()).thenReturn(Arrays.asList(columnNames));
		return new NebulaResultSet(nativeResultSet, null);
	}
}
