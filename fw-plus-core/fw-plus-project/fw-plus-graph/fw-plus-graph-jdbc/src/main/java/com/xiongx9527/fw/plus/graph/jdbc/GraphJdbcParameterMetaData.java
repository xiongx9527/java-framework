package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

public final class GraphJdbcParameterMetaData implements ParameterMetaData {

	private final int parameterCount;

	public GraphJdbcParameterMetaData(int parameterCount) {
		if (parameterCount < 0) {
			throw new IllegalArgumentException("parameterCount must not be negative");
		}
		this.parameterCount = parameterCount;
	}

	@Override
	public int getParameterCount() {
		return parameterCount;
	}

	@Override
	public int isNullable(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return parameterNullableUnknown;
	}

	@Override
	public boolean isSigned(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return false;
	}

	@Override
	public int getPrecision(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return 0;
	}

	@Override
	public int getScale(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return 0;
	}

	@Override
	public int getParameterType(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return Types.JAVA_OBJECT;
	}

	@Override
	public String getParameterTypeName(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return "JAVA_OBJECT";
	}

	@Override
	public String getParameterClassName(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return Object.class.getName();
	}

	@Override
	public int getParameterMode(int parameter) throws SQLException {
		checkParameterIndex(parameter);
		return parameterModeIn;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return GraphJdbcWrapperSupport.unwrap(this, iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return GraphJdbcWrapperSupport.isWrapperFor(this, iface);
	}

	private void checkParameterIndex(int parameter) throws SQLException {
		if (parameter < 1 || parameter > parameterCount) {
			throw new SQLException("Invalid parameter index: " + parameter);
		}
	}
}
