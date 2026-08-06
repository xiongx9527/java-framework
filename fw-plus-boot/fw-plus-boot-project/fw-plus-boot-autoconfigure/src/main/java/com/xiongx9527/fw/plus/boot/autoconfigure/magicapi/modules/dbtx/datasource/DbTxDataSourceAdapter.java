package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;

public interface DbTxDataSourceAdapter {

	String getCoordinator();

	DataSource prepare(String key, DataSource dataSource);

	void close(DataSource dataSource) throws Exception;
}
