package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.starter;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service.MybatisPlusXmlMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service.MybatisPlusXmlMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service.MybatisPlusXmlMapperRegistry;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.modules.db.SQLModule;

import javax.sql.DataSource;

@ConditionalOnProperty(prefix = "magic.api.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {
		"com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean",
		"com.github.yulichang.query.MPJQueryWrapper"
})
@Configuration
public class MybatisPlusConfiguration implements MagicPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-mybatisplus-xml.1.0.0.iife.js";

	@Bean
	@ConditionalOnMissingBean
	public MybatisPlusXmlMapperRegistry mybatisPlusXmlMapperRegistry(
			ObjectProvider<MybatisPlusModule> moduleProvider,
			MagicDynamicDataSource dynamicDataSource,
			@Autowired(required = false) DynamicEntityRegistry dynamicEntityRegistry) {
		return new MybatisPlusXmlMapperRegistry(moduleProvider, dynamicDataSource, dynamicEntityRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	public MybatisPlusXmlMagicResourceStorage mybatisPlusXmlMagicResourceStorage(MybatisPlusXmlMapperRegistry registry) {
		return new MybatisPlusXmlMagicResourceStorage(registry);
	}

	@Bean
	@ConditionalOnMissingBean
	public MybatisPlusXmlMagicDynamicRegistry mybatisPlusXmlMagicDynamicRegistry(
			MybatisPlusXmlMagicResourceStorage storage, MybatisPlusXmlMapperRegistry registry) {
		return new MybatisPlusXmlMagicDynamicRegistry(storage, registry);
	}

	@Bean
	public MybatisPlusModule mybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
											   ObjectProvider<TypeHandler[]> typeHandlersProvider,
											   ObjectProvider<DatabaseIdProvider> databaseIdProvider,
											   ObjectProvider<SQLModule> sqlModuleProvider,
											   ObjectProvider<MybatisPlusXmlMapperRegistry> xmlMapperRegistryProvider,
											   MagicDynamicDataSource dynamicDataSource,
											   MybatisPlusProperties mybatisPlusProperties,
											   @Autowired(required = false) DataSource dataSource) {
		if (dataSource instanceof DynamicRoutingDataSource
				&& ((DynamicRoutingDataSource) dataSource).getDataSources().isEmpty()) {
			dataSource = null;
		}
		return new MybatisPlusModule(interceptorsProvider, typeHandlersProvider, databaseIdProvider,
				sqlModuleProvider, xmlMapperRegistryProvider, dynamicDataSource, mybatisPlusProperties, dataSource);
	}

	/**
	 * 创建 MyBatis-Plus XML 插件。
	 *
	 * <p>始终上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物是否落在 classpath
	 * 上改变上报内容——产物在不在 classpath 是使用方的依赖配置问题，编码进注册逻辑只会让
	 * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
	 */
	@Override
	public Plugin plugin() {
		return new Plugin("mybatisplus-xml", "MagicMybatisPlusXml", JAVASCRIPT_FILENAME);
	}
}
