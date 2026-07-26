package com.xiongx9527.fw.plus.graph.jdbc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphJdbcParameterBindings {

	private final Map<Integer, Object> values = new LinkedHashMap<Integer, Object>();
	private final Map<Integer, Binding> bindings = new LinkedHashMap<Integer, Binding>();

	public void bind(int parameterIndex, Object value) {
		bind(parameterIndex, value, null, null);
	}

	public void bind(int parameterIndex, Object value, String setterName, Long length) {
		bind(parameterIndex, value, setterName, length, null, null);
	}

	public void bind(int parameterIndex, Object value, int jdbcType, Integer scaleOrLength) {
		bind(parameterIndex, value, null, null, Integer.valueOf(jdbcType), scaleOrLength);
	}

	private void bind(int parameterIndex, Object value, String setterName, Long length, Integer jdbcType,
			Integer scaleOrLength) {
		if (parameterIndex < 1) {
			throw new IllegalArgumentException("parameterIndex must be greater than 0");
		}
		Integer index = Integer.valueOf(parameterIndex);
		values.put(index, value);
		bindings.put(index, new Binding(value, setterName, length, jdbcType, scaleOrLength));
	}

	public Object get(int parameterIndex) {
		return values.get(Integer.valueOf(parameterIndex));
	}

	public Binding getBinding(int parameterIndex) {
		return bindings.get(Integer.valueOf(parameterIndex));
	}

	public Map<Integer, Object> asMap() {
		return Collections.unmodifiableMap(values);
	}

	public void clear() {
		values.clear();
		bindings.clear();
	}

	public int size() {
		return values.size();
	}

	public static int countQuestionMarks(String sql) {
		if (sql == null || sql.length() == 0) {
			return 0;
		}
		int count = 0;
		char quote = 0;
		for (int index = 0; index < sql.length(); index++) {
			char current = sql.charAt(index);
			if (quote != 0) {
				if (current == quote) {
					if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
						index++;
					}
					else {
						quote = 0;
					}
				}
				continue;
			}
			if (current == '\'' || current == '\"') {
				quote = current;
			}
			else if (current == '?') {
				count++;
			}
		}
		return count;
	}

	public static final class Binding {

		private final Object value;
		private final String setterName;
		private final Long length;
		private final Integer jdbcType;
		private final Integer scaleOrLength;

		private Binding(Object value, String setterName, Long length, Integer jdbcType, Integer scaleOrLength) {
			this.value = value;
			this.setterName = setterName;
			this.length = length;
			this.jdbcType = jdbcType;
			this.scaleOrLength = scaleOrLength;
		}

		public Object getValue() {
			return value;
		}

		public String getSetterName() {
			return setterName;
		}

		public Long getLength() {
			return length;
		}

		public Integer getJdbcType() {
			return jdbcType;
		}

		public Integer getScaleOrLength() {
			return scaleOrLength;
		}
	}
}
