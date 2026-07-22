package com.vesoft.nebula.jdbc.test;

import com.vesoft.nebula.jdbc.client.NebulaNativeRecord;
import com.vesoft.nebula.jdbc.client.NebulaNativeResultSet;
import com.vesoft.nebula.jdbc.client.NebulaNativeValue;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class NativeResultSets {
	private NativeResultSets() {
	}

	public static NebulaNativeResultSet success() throws SQLException {
		return rows(Arrays.<String>asList(), new String[0][0]);
	}

	public static NebulaNativeResultSet failure(Object errorCode, String errorMessage) throws SQLException {
		NebulaNativeResultSet resultSet = mock(NebulaNativeResultSet.class);
		when(resultSet.isSucceeded()).thenReturn(false);
		when(resultSet.isEmpty()).thenReturn(true);
		when(resultSet.getErrorCode()).thenReturn(errorCode);
		when(resultSet.getErrorMessage()).thenReturn(errorMessage);
		when(resultSet.getColumnNames()).thenReturn(new ArrayList<String>());
		when(resultSet.rows()).thenReturn(new ArrayList<NebulaNativeRecord>());
		return resultSet;
	}

	public static NebulaNativeResultSet rows(List<String> columnNames, String[][] values) throws SQLException {
		List<NebulaNativeRecord> records = new ArrayList<NebulaNativeRecord>();
		for (String[] row : values) {
			List<NebulaNativeValue> rowValues = new ArrayList<NebulaNativeValue>();
			for (String value : row) {
				rowValues.add(stringValue(value));
			}
			records.add(record(columnNames, rowValues));
		}
		return rows(columnNames, records);
	}

	public static NebulaNativeResultSet rows(List<String> columnNames, List<NebulaNativeRecord> records) throws SQLException {
		NebulaNativeResultSet resultSet = mock(NebulaNativeResultSet.class);
		when(resultSet.isSucceeded()).thenReturn(true);
		when(resultSet.isEmpty()).thenReturn(records.isEmpty());
		when(resultSet.getColumnNames()).thenReturn(columnNames);
		when(resultSet.rows()).thenReturn(records);
		return resultSet;
	}

	public static NebulaNativeRecord record(List<String> columnNames, List<NebulaNativeValue> values) throws SQLException {
		NebulaNativeRecord record = mock(NebulaNativeRecord.class);
		when(record.values()).thenReturn(values);
		for (int i = 0; i < values.size(); i++) {
			when(record.get(i)).thenReturn(values.get(i));
			if (i < columnNames.size()) {
				when(record.get(columnNames.get(i))).thenReturn(values.get(i));
			}
		}
		return record;
	}

	public static NebulaNativeValue stringValue(String value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isNull()).thenReturn(value == null);
		if (value != null) {
			when(valueWrapper.isString()).thenReturn(true);
			when(valueWrapper.asString()).thenReturn(value);
			when(valueWrapper.rawValue()).thenReturn(value);
		}
		return valueWrapper;
	}

	public static NebulaNativeValue longValue(long value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isLong()).thenReturn(true);
		when(valueWrapper.asLong()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(Long.valueOf(value));
		return valueWrapper;
	}

	public static NebulaNativeValue booleanValue(boolean value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isBoolean()).thenReturn(true);
		when(valueWrapper.asBoolean()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(Boolean.valueOf(value));
		return valueWrapper;
	}

	public static NebulaNativeValue doubleValue(double value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isDouble()).thenReturn(true);
		when(valueWrapper.asDouble()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(Double.valueOf(value));
		return valueWrapper;
	}

	public static NebulaNativeValue dateValue(Date value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isDate()).thenReturn(true);
		when(valueWrapper.asDate()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}

	public static NebulaNativeValue timeValue(Time value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isTime()).thenReturn(true);
		when(valueWrapper.asTime()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}

	public static NebulaNativeValue timestampValue(Timestamp value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.isDateTime()).thenReturn(true);
		when(valueWrapper.asTimestamp()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}

	public static NebulaNativeValue listValue(List value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.asList()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}

	public static NebulaNativeValue setValue(Set value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.asSet()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}

	public static NebulaNativeValue mapValue(Map value) throws SQLException {
		NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
		when(valueWrapper.asMap()).thenReturn(value);
		when(valueWrapper.rawValue()).thenReturn(value);
		return valueWrapper;
	}
}
