package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name =
		"com.xiongx9527.fw.plus.boot.starter.magicapi.dbtx.DbTxPluginMarker")
@ConditionalOnProperty(prefix = "magic-api.plugin.dbtx", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DbTxProperties.class)
public class MagicAPIDbTxConfiguration implements MagicPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "magic-api.plugin.dbtx", name = "local-enabled",
			havingValue = "true", matchIfMissing = true)
	public LocalDbTxCoordinator localDbTxCoordinator(MagicDynamicDataSource dynamicDataSource) {
		return new LocalDbTxCoordinator(dynamicDataSource);
	}

	@Bean
	@ConditionalOnMissingBean
	public DbTxCoordinatorRegistry dbTxCoordinatorRegistry(
			ObjectProvider<DbTxCoordinator> coordinators) {
		return new DbTxCoordinatorRegistry(coordinators.stream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnMissingBean
	public DbTxDataSourceDecorator dbTxDataSourceDecorator(
			ObjectProvider<DbTxDataSourceAdapter> adapters) {
		return new DbTxDataSourceDecorator(adapters.stream().collect(Collectors.toList()));
	}

	@Bean
	@ConditionalOnMissingBean
	public DbTxModule dbTxModule(DbTxCoordinatorRegistry coordinatorRegistry,
			DbTxProperties properties) {
		coordinatorRegistry.getRequired(properties.getDefaultCoordinator());
		return new DbTxModule(coordinatorRegistry, properties);
	}

	@Override
	public Plugin plugin() {
		return new Plugin("dbtx");
	}
}
