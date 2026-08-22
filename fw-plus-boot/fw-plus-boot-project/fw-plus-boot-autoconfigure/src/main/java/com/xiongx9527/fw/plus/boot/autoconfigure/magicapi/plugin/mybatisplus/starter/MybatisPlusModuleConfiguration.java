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
public class MybatisPlusModuleConfiguration implements MagicPluginConfiguration {

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

	@Override
	public Plugin plugin() {
		if (MybatisPlusModuleConfiguration.class.getClassLoader()
				.getResource("magic-editor/plugins/" + JAVASCRIPT_FILENAME) == null) {
			return new Plugin("mybatisplus-xml");
		}
		return new Plugin("mybatisplus-xml", "MagicMybatisPlusXml", JAVASCRIPT_FILENAME);
	}
}
