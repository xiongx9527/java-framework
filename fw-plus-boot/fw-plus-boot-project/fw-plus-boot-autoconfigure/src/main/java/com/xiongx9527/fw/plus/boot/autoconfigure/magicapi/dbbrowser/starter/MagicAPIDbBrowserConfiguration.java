package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service.DbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service.DbBrowserSqlBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service.DefaultDbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.web.MagicDbBrowserController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

@Configuration
@ConditionalOnProperty(prefix = "magic-api.plugin.db", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIDbBrowserConfiguration implements MagicPluginConfiguration {

	private final ObjectProvider<DbBrowserService> dbBrowserServiceProvider;

	public MagicAPIDbBrowserConfiguration(ObjectProvider<DbBrowserService> dbBrowserServiceProvider) {
		this.dbBrowserServiceProvider = dbBrowserServiceProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public DbBrowserSqlBuilder dbBrowserSqlBuilder() {
		return new DbBrowserSqlBuilder();
	}

	@Bean
	@ConditionalOnMissingBean(DbBrowserService.class)
	public DbBrowserService dbBrowserService(MagicDynamicDataSource dynamicDataSource, DbBrowserSqlBuilder sqlBuilder) {
		return new DefaultDbBrowserService(dynamicDataSource, sqlBuilder);
	}

	@Override
	public Plugin plugin() {
		return new Plugin("db", "MagicDB", "magic-db.1.0.0.iife.js");
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new MagicDbBrowserController(configuration, dbBrowserServiceProvider.getObject()));
	}
}
