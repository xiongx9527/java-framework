package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;

public interface MagicDataSourceDecorator {

	DataSource decorate(String key, DataSource dataSource);

	/**
	 * 回滚本装饰器成功返回的阶段资源，不得关闭输入的数据源链。
	 * 原始数据源由注册中心单独关闭；未成功返回的资源由装饰器在 decorate 内自行清理。
	 */
	default void cleanupAfterDecorationFailure(String key, DataSource dataSource) throws Exception {
	}

	/**
	 * 原子声明该阶段即将移除；任一后续步骤失败时由 afterRemoveFailure 撤销。
	 */
	default void startRemove(String key, DataSource dataSource) {
	}

	default void beforeRemove(String key, DataSource dataSource) {
	}

	/**
	 * 回滚 startRemove 产生的生命周期状态，不得关闭仍注册的数据源链。
	 */
	default void afterRemoveFailure(String key, DataSource dataSource) throws Exception {
	}
}
