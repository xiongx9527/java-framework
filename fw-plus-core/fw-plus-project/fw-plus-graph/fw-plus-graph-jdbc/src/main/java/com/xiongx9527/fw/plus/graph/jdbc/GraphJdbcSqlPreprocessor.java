package com.xiongx9527.fw.plus.graph.jdbc;

import java.sql.SQLException;

public interface GraphJdbcSqlPreprocessor {

	String preprocess(String sql) throws SQLException;
}
