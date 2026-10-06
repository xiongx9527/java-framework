package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserSqlBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DefaultDbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.web.MagicDbBrowserController;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

@Configuration
@ConditionalOnClass({MagicPluginConfiguration.class, MagicDynamicDataSource.class})
@ConditionalOnProperty(prefix = "magic-api.plugin.db", name = "enabled", havingValue = "true", matchIfMissing = true)
@Conditional(MagicAPIDbBrowserAdminEnabledCondition.class)
public class MagicAPIDbBrowserConfiguration implements MagicPluginConfiguration {
	private static final String DB_BROWSER_JAVASCRIPT = "magic-db.1.0.0.iife.js";

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

	/**
	 * 创建数据库浏览器插件。
	 *
	 * <p>始终上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物是否落在 classpath
	 * 上改变上报内容——产物在不在 classpath 是使用方的依赖配置问题，编码进注册逻辑只会让
	 * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
	 */
	@Override
	public Plugin plugin() {
		Plugin plugin = new Plugin("db", "MagicDB", DB_BROWSER_JAVASCRIPT);
		String adminEnabled = SpringUtils.getAppConfig("magic-api.plugin.db.admin.enabled", "false");
		plugin.setAdminEnabled(Boolean.parseBoolean(adminEnabled));
		return plugin;
	}

	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new MagicDbBrowserController(configuration, dbBrowserServiceProvider.getObject()));
	}
}
