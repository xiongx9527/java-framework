package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;

public interface DbTxDataSourceAdapter {

	String getCoordinator();

	DataSource prepare(String key, DataSource dataSource);

	default void beforeRemove(DataSource dataSource) throws Exception {
	}

	void close(DataSource dataSource) throws Exception;
}
