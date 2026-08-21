package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import java.sql.SQLException;
import java.util.List;

public interface NebulaNativeResultSet {
	boolean isSucceeded() throws SQLException;

	boolean isEmpty() throws SQLException;

	Object getErrorCode() throws SQLException;

	String getErrorMessage() throws SQLException;

	List<String> getColumnNames() throws SQLException;

	List<NebulaNativeRecord> rows() throws SQLException;

	Object unwrap();
}
