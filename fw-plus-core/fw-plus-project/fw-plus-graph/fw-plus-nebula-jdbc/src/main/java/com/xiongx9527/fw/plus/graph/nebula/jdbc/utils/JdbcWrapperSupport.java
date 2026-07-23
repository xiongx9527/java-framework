package com.xiongx9527.fw.plus.graph.nebula.jdbc.utils;

import java.sql.SQLException;

public final class JdbcWrapperSupport {
	private JdbcWrapperSupport() {
	}

	public static <T> T unwrap(Object target, Class<T> iface) throws SQLException {
		if (iface == null) {
			throw new SQLException("Interface must not be null.");
		}
		if (iface.isInstance(target)) {
			return iface.cast(target);
		}
		throw new SQLException("Cannot unwrap " + target.getClass().getName() + " to " + iface.getName() + ".");
	}

	public static boolean isWrapperFor(Object target, Class<?> iface) {
		return iface != null && iface.isInstance(target);
	}
}
