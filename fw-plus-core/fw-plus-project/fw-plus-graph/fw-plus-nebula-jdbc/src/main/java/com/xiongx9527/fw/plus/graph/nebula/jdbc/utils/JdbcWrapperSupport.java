package com.xiongx9527.fw.plus.graph.nebula.jdbc.utils;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcWrapperSupport;
import java.sql.SQLException;

public final class JdbcWrapperSupport {
	private JdbcWrapperSupport() {
	}

	public static <T> T unwrap(Object target, Class<T> iface) throws SQLException {
		if (iface != null && !GraphJdbcWrapperSupport.isWrapperFor(target, iface)) {
			throw new SQLException("Cannot unwrap " + (target == null ? "null" : target.getClass().getName())
					+ " to " + iface.getName() + ".");
		}
		return GraphJdbcWrapperSupport.unwrap(target, iface);
	}

	public static boolean isWrapperFor(Object target, Class<?> iface) {
		return GraphJdbcWrapperSupport.isWrapperFor(target, iface);
	}
}
