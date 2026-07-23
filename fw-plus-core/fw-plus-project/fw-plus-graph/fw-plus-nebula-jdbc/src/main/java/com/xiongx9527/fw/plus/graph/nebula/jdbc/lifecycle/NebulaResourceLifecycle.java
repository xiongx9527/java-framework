package com.xiongx9527.fw.plus.graph.nebula.jdbc.lifecycle;

import java.sql.SQLException;

public class NebulaResourceLifecycle {
    private boolean closed;

    @FunctionalInterface
    public interface ThrowingCloseAction {
        void close() throws SQLException;
    }

    public void close(ThrowingCloseAction closeAction) throws SQLException {
        if (closed) {
            return;
        }
        try {
            closeAction.close();
            closed = true;
        } catch (SQLException exception) {
            closed = true;
            throw exception;
        }
    }

    public void checkOpen(String resourceName) throws SQLException {
        if (closed) {
            throw new SQLException(resourceName + " already closed.");
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
