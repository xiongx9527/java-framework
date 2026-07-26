package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.SQLException;

public final class GraphJdbcLifecycle {

	private boolean closed;

	public interface ThrowingCloseAction {
		void close() throws SQLException;
	}

	public synchronized void close(ThrowingCloseAction action) throws SQLException {
		if (closed) {
			return;
		}
		try {
			if (action != null) {
				action.close();
			}
		}
		finally {
			closed = true;
		}
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	public synchronized void checkOpen(String resourceName) throws SQLException {
		if (closed) {
			throw new SQLException(resourceName + " is closed");
		}
	}
}
