package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import java.sql.SQLException;

public interface NebulaNativeClient extends AutoCloseable {
	void open(NebulaConnectionConfigHolder config) throws SQLException;

	NebulaNativeResultSet execute(String ngql) throws SQLException;

	Object getNativeClient() throws SQLException;

	Object getNativeSession() throws SQLException;

	Object getNativePool() throws SQLException;

	boolean isClosed();

	@Override
	void close();
}
