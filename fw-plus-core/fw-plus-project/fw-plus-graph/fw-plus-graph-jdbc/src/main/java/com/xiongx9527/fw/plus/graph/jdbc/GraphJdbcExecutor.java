package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.SQLException;

public interface GraphJdbcExecutor {

	GraphJdbcResult execute(String sql, GraphJdbcParameterBindings bindings) throws SQLException;
}
