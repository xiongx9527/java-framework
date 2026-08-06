package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;

public interface DbTxDataSourceAdapter {

	String getCoordinator();

	DataSource prepare(String key, DataSource dataSource);

	default void beforeRemove(DataSource dataSource) throws Exception {
	}

	void close(DataSource dataSource) throws Exception;

	/**
	 * @return true when this adapter owns raw DataSource close handling, including an intentional no-op
	 */
	default boolean closeRawDataSource(DataSource dataSource) throws Exception {
		return false;
	}
}
