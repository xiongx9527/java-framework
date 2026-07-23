package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import java.sql.SQLException;
import java.util.List;

public interface NebulaNativeRecord {
	NebulaNativeValue get(int columnIndex) throws SQLException;

	NebulaNativeValue get(String columnName) throws SQLException;

	List<NebulaNativeValue> values() throws SQLException;

	Object unwrap();
}
