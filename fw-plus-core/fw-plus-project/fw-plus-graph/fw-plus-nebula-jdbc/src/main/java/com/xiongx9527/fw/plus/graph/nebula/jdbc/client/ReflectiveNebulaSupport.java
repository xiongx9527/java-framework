package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ReflectiveNebulaSupport {
	private ReflectiveNebulaSupport() {
	}

	static Class<?> type(String className) throws SQLException {
		try {
			return Class.forName(className, false, classLoader());
		} catch (ClassNotFoundException exception) {
			throw new SQLException("Nebula Java SDK class not found: " + className, exception);
		}
	}

	static Object newInstance(String className) throws SQLException {
		try {
			return type(className).newInstance();
		} catch (Exception exception) {
			throw sqlException(exception);
		}
	}

	static Object newInstance(String className, Class<?>[] parameterTypes, Object[] args) throws SQLException {
		try {
			Constructor<?> constructor = type(className).getConstructor(parameterTypes);
			return constructor.newInstance(args);
		} catch (Exception exception) {
			throw sqlException(exception);
		}
	}

	static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws SQLException {
		try {
			Method method = target.getClass().getMethod(methodName, parameterTypes);
			return method.invoke(target, args);
		} catch (NoSuchMethodException exception) {
			throw new SQLException("Nebula Java SDK method not found: " + target.getClass().getName() + "." + methodName, exception);
		} catch (Exception exception) {
			throw sqlException(exception);
		}
	}

	static Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object[] args)
			throws SQLException {
		try {
			Method method = type(className).getMethod(methodName, parameterTypes);
			return method.invoke(null, args);
		} catch (NoSuchMethodException exception) {
			throw new SQLException("Nebula Java SDK method not found: " + className + "." + methodName, exception);
		} catch (Exception exception) {
			throw sqlException(exception);
		}
	}

	static boolean hasMethod(Object target, String methodName, Class<?>... parameterTypes) {
		try {
			target.getClass().getMethod(methodName, parameterTypes);
			return true;
		} catch (NoSuchMethodException exception) {
			return false;
		}
	}

	static SQLException sqlException(Exception exception) {
		Throwable cause = exception instanceof InvocationTargetException
				? ((InvocationTargetException) exception).getTargetException()
				: exception;
		if (cause instanceof SQLException) {
			return (SQLException) cause;
		}
		return new SQLException(cause.getMessage(), cause);
	}

	private static ClassLoader classLoader() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader == null ? ReflectiveNebulaSupport.class.getClassLoader() : classLoader;
	}

	static final class ResultSetAdapter implements NebulaNativeResultSet {
		private final Object resultSet;
		private List<NebulaNativeRecord> rows;

		ResultSetAdapter(Object resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public boolean isSucceeded() throws SQLException {
			return Boolean.TRUE.equals(invoke(resultSet, "isSucceeded", new Class<?>[0], new Object[0]));
		}

		@Override
		public boolean isEmpty() throws SQLException {
			return Boolean.TRUE.equals(invoke(resultSet, "isEmpty", new Class<?>[0], new Object[0]));
		}

		@Override
		public Object getErrorCode() throws SQLException {
			return invoke(resultSet, "getErrorCode", new Class<?>[0], new Object[0]);
		}

		@Override
		public String getErrorMessage() throws SQLException {
			Object value = invoke(resultSet, "getErrorMessage", new Class<?>[0], new Object[0]);
			return value == null ? null : String.valueOf(value);
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<String> getColumnNames() throws SQLException {
			Object value = invoke(resultSet, "getColumnNames", new Class<?>[0], new Object[0]);
			return value == null ? new ArrayList<String>() : new ArrayList<String>((List<String>) value);
		}

		@Override
		public List<NebulaNativeRecord> rows() throws SQLException {
			if (rows == null) {
				rows = loadRows();
			}
			return new ArrayList<NebulaNativeRecord>(rows);
		}

		private List<NebulaNativeRecord> loadRows() throws SQLException {
			List<NebulaNativeRecord> materialized = new ArrayList<NebulaNativeRecord>();
			if (isEmpty()) {
				return materialized;
			}
			if (hasMethod(resultSet, "hasNext") && hasMethod(resultSet, "next")) {
				while (Boolean.TRUE.equals(invoke(resultSet, "hasNext", new Class<?>[0], new Object[0]))) {
					materialized.add(new RecordAdapter(invoke(resultSet, "next", new Class<?>[0], new Object[0])));
				}
				return materialized;
			}
			int rowSize = ((Number) invoke(resultSet, "rowsSize", new Class<?>[0], new Object[0])).intValue();
			for (int i = 0; i < rowSize; i++) {
				materialized.add(new RecordAdapter(invoke(resultSet, "rowValues",
						new Class<?>[] {int.class}, new Object[] {Integer.valueOf(i)})));
			}
			return materialized;
		}

		@Override
		public Object unwrap() {
			return resultSet;
		}
	}

	static final class RecordAdapter implements NebulaNativeRecord {
		private final Object record;

		RecordAdapter(Object record) {
			this.record = record;
		}

		@Override
		public NebulaNativeValue get(int columnIndex) throws SQLException {
			Object value = invoke(record, "get", new Class<?>[] {int.class}, new Object[] {Integer.valueOf(columnIndex)});
			return value == null ? null : new ValueAdapter(value);
		}

		@Override
		public NebulaNativeValue get(String columnName) throws SQLException {
			Object value = invoke(record, "get", new Class<?>[] {String.class}, new Object[] {columnName});
			return value == null ? null : new ValueAdapter(value);
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<NebulaNativeValue> values() throws SQLException {
			List<Object> values = (List<Object>) invoke(record, "values", new Class<?>[0], new Object[0]);
			List<NebulaNativeValue> wrapped = new ArrayList<NebulaNativeValue>();
			if (values != null) {
				for (Object value : values) {
					wrapped.add(value == null ? null : new ValueAdapter(value));
				}
			}
			return wrapped;
		}

		@Override
		public Object unwrap() {
			return record;
		}
	}

	static final class ValueAdapter implements NebulaNativeValue {
		private final Object valueWrapper;

		ValueAdapter(Object valueWrapper) {
			this.valueWrapper = valueWrapper;
		}

		@Override
		public boolean isNull() {
			return booleanMethod("isNull");
		}

		@Override
		public boolean isString() {
			return booleanMethod("isString");
		}

		@Override
		public boolean isLong() {
			return booleanMethod("isLong");
		}

		@Override
		public boolean isInt() {
			return booleanMethod("isInt");
		}

		@Override
		public boolean isFloat() {
			return booleanMethod("isFloat");
		}

		@Override
		public boolean isDouble() {
			return booleanMethod("isDouble");
		}

		@Override
		public boolean isBoolean() {
			return booleanMethod("isBoolean");
		}

		@Override
		public boolean isDate() {
			return booleanMethod("isDate");
		}

		@Override
		public boolean isTime() {
			return booleanMethod("isTime") || booleanMethod("isLocalTime") || booleanMethod("isZonedTime");
		}

		@Override
		public boolean isDateTime() {
			return booleanMethod("isDateTime") || booleanMethod("isLocalDateTime") || booleanMethod("isZonedDateTime");
		}

		@Override
		public String asString() throws SQLException {
			Object value = invoke(valueWrapper, "asString", new Class<?>[0], new Object[0]);
			return value == null ? null : String.valueOf(value);
		}

		@Override
		public long asLong() throws SQLException {
			return ((Number) invoke(valueWrapper, "asLong", new Class<?>[0], new Object[0])).longValue();
		}

		@Override
		public int asInt() throws SQLException {
			if (hasMethod(valueWrapper, "asInt")) {
				return ((Number) invoke(valueWrapper, "asInt", new Class<?>[0], new Object[0])).intValue();
			}
			return (int) asLong();
		}

		@Override
		public float asFloat() throws SQLException {
			if (hasMethod(valueWrapper, "asFloat")) {
				return ((Number) invoke(valueWrapper, "asFloat", new Class<?>[0], new Object[0])).floatValue();
			}
			return (float) asDouble();
		}

		@Override
		public double asDouble() throws SQLException {
			return ((Number) invoke(valueWrapper, "asDouble", new Class<?>[0], new Object[0])).doubleValue();
		}

		@Override
		public boolean asBoolean() throws SQLException {
			return Boolean.TRUE.equals(invoke(valueWrapper, "asBoolean", new Class<?>[0], new Object[0]));
		}

		@Override
		public Date asDate() throws SQLException {
			Object value = invoke(valueWrapper, "asDate", new Class<?>[0], new Object[0]);
			if (value == null) {
				return null;
			}
			if (value instanceof LocalDate) {
				return Date.valueOf((LocalDate) value);
			}
			int year = ((Number) invoke(value, "getYear", new Class<?>[0], new Object[0])).intValue();
			int month = ((Number) invoke(value, "getMonth", new Class<?>[0], new Object[0])).intValue();
			int day = ((Number) invoke(value, "getDay", new Class<?>[0], new Object[0])).intValue();
			return Date.valueOf(LocalDate.of(year, month, day));
		}

		@Override
		public Time asTime() throws SQLException {
			Object value;
			if (hasMethod(valueWrapper, "asZonedTime") && booleanMethod("isZonedTime")) {
				value = invoke(valueWrapper, "asZonedTime", new Class<?>[0], new Object[0]);
			} else if (hasMethod(valueWrapper, "asLocalTime")) {
				value = invoke(valueWrapper, "asLocalTime", new Class<?>[0], new Object[0]);
			} else {
				value = invoke(valueWrapper, "asTime", new Class<?>[0], new Object[0]);
			}
			if (value == null) {
				return null;
			}
			if (value instanceof OffsetTime) {
				return Time.valueOf(((OffsetTime) value).toLocalTime());
			}
			if (value instanceof LocalTime) {
				return Time.valueOf((LocalTime) value);
			}
			int hour = ((Number) invoke(value, "getHour", new Class<?>[0], new Object[0])).intValue();
			int minute = ((Number) invoke(value, "getMinute", new Class<?>[0], new Object[0])).intValue();
			int second = ((Number) invoke(value, "getSecond", new Class<?>[0], new Object[0])).intValue();
			return Time.valueOf(LocalTime.of(hour, minute, second));
		}

		@Override
		public Timestamp asTimestamp() throws SQLException {
			Object value;
			if (hasMethod(valueWrapper, "asZonedDateTime") && booleanMethod("isZonedDateTime")) {
				value = invoke(valueWrapper, "asZonedDateTime", new Class<?>[0], new Object[0]);
			} else if (hasMethod(valueWrapper, "asLocalDateTime")) {
				value = invoke(valueWrapper, "asLocalDateTime", new Class<?>[0], new Object[0]);
			} else {
				value = invoke(valueWrapper, "asDateTime", new Class<?>[0], new Object[0]);
			}
			if (value == null) {
				return null;
			}
			if (value instanceof ZonedDateTime) {
				return Timestamp.valueOf(((ZonedDateTime) value).toLocalDateTime());
			}
			if (value instanceof LocalDateTime) {
				return Timestamp.valueOf((LocalDateTime) value);
			}
			int year = ((Number) invoke(value, "getYear", new Class<?>[0], new Object[0])).intValue();
			int month = ((Number) invoke(value, "getMonth", new Class<?>[0], new Object[0])).intValue();
			int day = ((Number) invoke(value, "getDay", new Class<?>[0], new Object[0])).intValue();
			int hour = ((Number) invoke(value, "getHour", new Class<?>[0], new Object[0])).intValue();
			int minute = ((Number) invoke(value, "getMinute", new Class<?>[0], new Object[0])).intValue();
			int second = ((Number) invoke(value, "getSecond", new Class<?>[0], new Object[0])).intValue();
			return Timestamp.valueOf(LocalDateTime.of(year, month, day, hour, minute, second));
		}

		@Override
		public List asList() throws SQLException {
			return unwrapList((List) invoke(valueWrapper, "asList", new Class<?>[0], new Object[0]));
		}

		@Override
		public Set asSet() throws SQLException {
			return unwrapSet((Set) invoke(valueWrapper, "asSet", new Class<?>[0], new Object[0]));
		}

		@Override
		public Map asMap() throws SQLException {
			return unwrapMap((Map) invoke(valueWrapper, "asMap", new Class<?>[0], new Object[0]));
		}

		@Override
		public Object asNode() throws SQLException {
			return invoke(valueWrapper, "asNode", new Class<?>[0], new Object[0]);
		}

		@Override
		public Object asEdge() throws SQLException {
			if (hasMethod(valueWrapper, "asEdge")) {
				return invoke(valueWrapper, "asEdge", new Class<?>[0], new Object[0]);
			}
			return invoke(valueWrapper, "asRelationship", new Class<?>[0], new Object[0]);
		}

		@Override
		public Object asPath() throws SQLException {
			return invoke(valueWrapper, "asPath", new Class<?>[0], new Object[0]);
		}

		@Override
		public Object rawValue() {
			try {
				return invoke(valueWrapper, "getValue", new Class<?>[0], new Object[0]);
			} catch (SQLException exception) {
				return valueWrapper;
			}
		}

		@Override
		public Object unwrap() {
			return valueWrapper;
		}

		private boolean booleanMethod(String methodName) {
			try {
				if (!hasMethod(valueWrapper, methodName)) {
					return false;
				}
				return Boolean.TRUE.equals(invoke(valueWrapper, methodName, new Class<?>[0], new Object[0]));
			} catch (SQLException exception) {
				return false;
			}
		}

		private Object unwrapNested(Object value) throws SQLException {
			if (value == null) {
				return null;
			}
			if (hasMethod(value, "isNull") && hasMethod(value, "getValue")) {
				return new ValueAdapter(value).rawValue();
			}
			return value;
		}

		private List unwrapList(List values) throws SQLException {
			if (values == null) {
				return null;
			}
			List unwrapped = new ArrayList();
			for (Object value : values) {
				unwrapped.add(unwrapNested(value));
			}
			return unwrapped;
		}

		private Set unwrapSet(Set values) throws SQLException {
			if (values == null) {
				return null;
			}
			Set unwrapped = new HashSet();
			for (Object value : values) {
				unwrapped.add(unwrapNested(value));
			}
			return unwrapped;
		}

		private Map unwrapMap(Map values) throws SQLException {
			if (values == null) {
				return null;
			}
			Map unwrapped = new HashMap();
			for (Object entryObject : values.entrySet()) {
				Map.Entry entry = (Map.Entry) entryObject;
				unwrapped.put(entry.getKey(), unwrapNested(entry.getValue()));
			}
			return unwrapped;
		}
	}
}
