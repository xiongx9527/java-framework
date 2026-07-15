package com.vesoft.nebula.jdbc.lifecycle;

import java.sql.SQLException;

public class NebulaResourceLifecycle {
    private boolean closed;

    public void close(ThrowingCloseAction closeAction) throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        closeAction.close();
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
