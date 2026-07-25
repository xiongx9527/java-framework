package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserSqlBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DefaultDbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.web.MagicDbBrowserController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

@Configuration
@ConditionalOnClass({MagicPluginConfiguration.class, MagicDynamicDataSource.class})
@ConditionalOnProperty(prefix = "magic-api.plugin.db", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIDbBrowserConfiguration implements MagicPluginConfiguration {
	private static final String DB_BROWSER_JAVASCRIPT = "magic-db.1.0.0.iife.js";
	private static final String DB_BROWSER_RESOURCE = "magic-editor/plugins/" + DB_BROWSER_JAVASCRIPT;

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
		if (MagicAPIDbBrowserConfiguration.class.getClassLoader().getResource(DB_BROWSER_RESOURCE) == null) {
			return new Plugin("db");
		}
		return new Plugin("db", "MagicDB", DB_BROWSER_JAVASCRIPT);
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new MagicDbBrowserController(configuration, dbBrowserServiceProvider.getObject()));
	}
}
