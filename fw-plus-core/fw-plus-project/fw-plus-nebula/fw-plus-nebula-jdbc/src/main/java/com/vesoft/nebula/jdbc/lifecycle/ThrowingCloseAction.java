package com.vesoft.nebula.jdbc.lifecycle;

import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingCloseAction {
    void close() throws SQLException;
}
