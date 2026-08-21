package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcParameterBindings;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaParameterBindings {
	private static final NebulaParameterBindings EMPTY = new NebulaParameterBindings(0);

	private final int size;
	private final GraphJdbcParameterBindings delegate = new GraphJdbcParameterBindings();

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
		bind(parameterIndex, value, jdbcType, null);
	}

	public void bind(int parameterIndex, Object value, int jdbcType, Integer scaleOrLength) throws SQLException {
		checkIndex(parameterIndex);
		delegate.bind(parameterIndex, value, jdbcType, scaleOrLength);
	}

	public NebulaParameterBinding get(int parameterIndex) throws SQLException {
		checkIndex(parameterIndex);
		return toNebulaBinding(delegate.getBinding(parameterIndex));
	}

	public NebulaParameterBindings copy() throws SQLException {
		NebulaParameterBindings copy = new NebulaParameterBindings(size);
		for (Map.Entry<Integer, NebulaParameterBinding> entry : asMap().entrySet()) {
			copy.bind(
					entry.getKey(),
					entry.getValue().getValue(),
					entry.getValue().getJdbcType(),
					entry.getValue().getScaleOrLength()
			);
		}
		return copy;
	}

	public Map<Integer, NebulaParameterBinding> asMap() {
		Map<Integer, NebulaParameterBinding> bindings = new LinkedHashMap<Integer, NebulaParameterBinding>();
		for (Map.Entry<Integer, Object> entry : delegate.asMap().entrySet()) {
			bindings.put(entry.getKey(), toNebulaBinding(delegate.getBinding(entry.getKey().intValue())));
		}
		return Collections.unmodifiableMap(bindings);
	}

	public void validateComplete() throws SQLException {
		for (int index = 1; index <= size; index++) {
			if (delegate.getBinding(index) == null) {
				throw new SQLException("Parameter " + index + " is not bound.");
			}
		}
	}

	private void checkIndex(int parameterIndex) throws SQLException {
		if (parameterIndex < 1 || parameterIndex > size) {
			throw new SQLException("Parameter index " + parameterIndex + " is out of range. Parameter count: " + size + ".");
		}
	}

	private NebulaParameterBinding toNebulaBinding(GraphJdbcParameterBindings.Binding binding) {
		if (binding == null) {
			return null;
		}
		return new NebulaParameterBinding(binding.getValue(), binding.getJdbcType().intValue(), binding.getScaleOrLength());
	}
}
