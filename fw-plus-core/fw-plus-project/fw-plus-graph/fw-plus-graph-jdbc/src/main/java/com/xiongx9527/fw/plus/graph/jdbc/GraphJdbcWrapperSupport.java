package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.SQLException;

public final class GraphJdbcWrapperSupport {

	private GraphJdbcWrapperSupport() {
	}

	public static <T> T unwrap(Object target, Class<T> iface) throws SQLException {
		if (isWrapperFor(target, iface)) {
			return iface.cast(target);
		}
		throw new SQLException("No wrapped object for " + (iface == null ? "null" : iface.getName()));
	}

	public static boolean isWrapperFor(Object target, Class<?> iface) {
		return iface != null && target != null && iface.isInstance(target);
	}
}
