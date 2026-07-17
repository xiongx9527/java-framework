package com.vesoft.nebula.jdbc.sql;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaParameterBindings {
	private static final NebulaParameterBindings EMPTY = new NebulaParameterBindings(0);

	private final int size;
	private final Map<Integer, NebulaParameterBinding> values = new LinkedHashMap<Integer, NebulaParameterBinding>();

	public NebulaParameterBindings(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("size must be >= 0");
		}
		this.size = size;
	}

	public static NebulaParameterBindings empty() {
		return EMPTY;
	}

	public int size() {
		return size;
	}

	public void bind(int parameterIndex, Object value, int jdbcType) throws SQLException {
		checkIndex(parameterIndex);
		values.put(parameterIndex, new NebulaParameterBinding(value, jdbcType));
	}

	public NebulaParameterBinding get(int parameterIndex) throws SQLException {
		checkIndex(parameterIndex);
		return values.get(parameterIndex);
	}

	public NebulaParameterBindings copy() throws SQLException {
		NebulaParameterBindings copy = new NebulaParameterBindings(size);
		for (Map.Entry<Integer, NebulaParameterBinding> entry : values.entrySet()) {
			copy.bind(entry.getKey(), entry.getValue().getValue(), entry.getValue().getJdbcType());
		}
		return copy;
	}

	public void validateComplete() throws SQLException {
		for (int index = 1; index <= size; index++) {
			if (!values.containsKey(index)) {
				throw new SQLException("Parameter " + index + " is not bound.");
			}
		}
	}

	private void checkIndex(int parameterIndex) throws SQLException {
		if (parameterIndex < 1 || parameterIndex > size) {
			throw new SQLException("Parameter index " + parameterIndex + " is out of range. Parameter count: " + size + ".");
		}
	}
}
