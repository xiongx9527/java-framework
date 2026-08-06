package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "magic-api.dbtx", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DbTxProperties.class)
public class MagicAPIDbTxConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "magic-api.dbtx", name = "local-enabled",
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
	public SmartInitializingSingleton dbTxDefaultCoordinatorValidator(
			DbTxCoordinatorRegistry coordinatorRegistry, DbTxProperties properties) {
		return () -> coordinatorRegistry.getRequired(properties.getDefaultCoordinator());
	}

	@Bean
	@ConditionalOnMissingBean
	public DbTxModule dbTxModule(DbTxCoordinatorRegistry coordinatorRegistry,
			DbTxProperties properties) {
		return new DbTxModule(coordinatorRegistry, properties);
	}
}
