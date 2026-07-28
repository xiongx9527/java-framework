package com.xiongx9527.fw.plus.graph.nebula.jdbc.lifecycle;

import java.sql.SQLException;

@FunctionalInterface
public interface ThrowingCloseAction {
    void close() throws SQLException;
}
