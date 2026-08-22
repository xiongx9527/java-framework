package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.SeataTransactionFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.datasource.SeataDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.starter.MagicAPISeataConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIDbTxConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MagicAPIDbTxConfiguration.class, MagicAPISeataConfiguration.class))
			.withUserConfiguration(DynamicDataSourceConfiguration.class);
	private final WebApplicationContextRunner baseContextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class))
			.withInitializer(context -> context.addBeanFactoryPostProcessor(
					new LazyInitializationBeanFactoryPostProcessor()))
			.withUserConfiguration(DynamicDataSourceConfiguration.class);

	@Test
	void disabledDbTxDoesNotCreateDbTxBeansEvenWhenSeataIsEnabled() {
		contextRunner.withPropertyValues(
				"magic-api.dbtx.enabled=false",
				"magic-api.dbtx.seata-enabled=true")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(DbTxModule.class);
					assertThat(context).doesNotHaveBean(DbTxCoordinatorRegistry.class);
					assertThat(context).doesNotHaveBean(LocalDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(DbTxDataSourceDecorator.class);
					assertThat(context).doesNotHaveBean(SeataTransactionFactory.class);
					assertThat(context).doesNotHaveBean(SeataDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDataSourceAdapter.class);
				});
	}

	@Test
	void baseAutoConfigurationCreatesLocalModuleByDefault() {
		baseContextRunner.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(DbTxProperties.class);
					assertThat(context).hasSingleBean(DbTxModule.class);
					assertThat(context).hasSingleBean(DbTxCoordinatorRegistry.class);
					assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
					assertThat(context).hasSingleBean(DbTxDataSourceDecorator.class);
				});
	}

	@Test
	void disabledLocalCoordinatorCannotRemainTheDefault() {
		contextRunner.withPropertyValues("magic-api.dbtx.local-enabled=false")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(DbTxException.class)
							.hasMessage("Coordinator is unavailable: local");
				});
	}

	@Test
	void customModuleCannotBypassDefaultCoordinatorValidation() {
		contextRunner.withUserConfiguration(CustomDbTxModuleConfiguration.class)
				.withPropertyValues("magic-api.dbtx.local-enabled=false")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(DbTxException.class)
							.hasMessage("Coordinator is unavailable: local");
				});
	}

	@Test
	void seataCannotBeTheDefaultWhenUnavailable() {
		contextRunner.withPropertyValues("magic-api.dbtx.default-coordinator=seata")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(DbTxException.class)
							.hasMessage("Coordinator is unavailable: seata");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class DynamicDataSourceConfiguration {

		@Bean
		MagicDynamicDataSource magicDynamicDataSource() {
			return new MagicDynamicDataSource();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDbTxModuleConfiguration {

		@Bean
		DbTxModule customDbTxModule(DbTxCoordinatorRegistry coordinatorRegistry,
				DbTxProperties properties) {
			return new DbTxModule(coordinatorRegistry, properties);
		}
	}
}
