package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;

public final class GraphJdbcDefaults {

	private GraphJdbcDefaults() {
	}

	public static SQLException unsupportedOperation() {
		return new SQLFeatureNotSupportedException("Unsupported JDBC feature");
	}

	public static Object defaultValue(Class<?> returnType) {
		if (returnType == Void.TYPE) {
			return null;
		}
		if (returnType == Boolean.TYPE) {
			return Boolean.FALSE;
		}
		if (returnType == Integer.TYPE) {
			return Integer.valueOf(0);
		}
		if (returnType == Long.TYPE) {
			return Long.valueOf(0L);
		}
		if (returnType == Float.TYPE) {
			return Float.valueOf(0F);
		}
		if (returnType == Double.TYPE) {
			return Double.valueOf(0D);
		}
		if (returnType == Short.TYPE) {
			return Short.valueOf((short) 0);
		}
		if (returnType == Byte.TYPE) {
			return Byte.valueOf((byte) 0);
		}
		return null;
	}

	public static int jdbcType(Object value) {
		if (value instanceof Integer) {
			return Types.INTEGER;
		}
		if (value instanceof Long) {
			return Types.BIGINT;
		}
		if (value instanceof Float || value instanceof Double) {
			return Types.DOUBLE;
		}
		if (value instanceof Boolean) {
			return Types.BOOLEAN;
		}
		return Types.VARCHAR;
	}

}
