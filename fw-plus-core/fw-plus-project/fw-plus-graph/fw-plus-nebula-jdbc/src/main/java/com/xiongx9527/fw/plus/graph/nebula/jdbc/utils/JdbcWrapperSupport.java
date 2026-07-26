package com.xiongx9527.fw.plus.graph.nebula.jdbc.utils;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcWrapperSupport;
import java.sql.SQLException;

public final class JdbcWrapperSupport {
	private JdbcWrapperSupport() {
	}

	public static <T> T unwrap(Object target, Class<T> iface) throws SQLException {
		return GraphJdbcWrapperSupport.unwrap(target, iface);
	}

	public static boolean isWrapperFor(Object target, Class<?> iface) {
		return GraphJdbcWrapperSupport.isWrapperFor(target, iface);
	}
}
