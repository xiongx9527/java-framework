package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;

public interface MagicDataSourceDecorator {

	DataSource decorate(String key, DataSource dataSource);

	default void beforeRemove(String key, DataSource dataSource) {
	}
}
