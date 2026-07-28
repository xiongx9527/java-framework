package com.xiongx9527.fw.plus.graph.nebula.jdbc.lifecycle;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcLifecycle;
import java.sql.SQLException;

public class NebulaResourceLifecycle {
	private final GraphJdbcLifecycle delegate = new GraphJdbcLifecycle();

    @FunctionalInterface
    public interface ThrowingCloseAction {
        void close() throws SQLException;
    }

    public void close(final ThrowingCloseAction closeAction) throws SQLException {
		delegate.close(closeAction == null ? null : new GraphJdbcLifecycle.ThrowingCloseAction() {
			@Override
			public void close() throws SQLException {
				closeAction.close();
			}
		});
    }

    public void checkOpen(String resourceName) throws SQLException {
		delegate.checkOpen(resourceName);
    }

    public boolean isClosed() {
		return delegate.isClosed();
    }
}
