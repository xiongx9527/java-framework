package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.datasource.SeataDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.starter.MagicAPISeataConfiguration;
import com.xiongx9527.fw.plus.boot.starter.magicapi.dbtx.DbTxPluginMarker;
import com.xiongx9527.fw.plus.boot.starter.magicapi.seata.SeataPluginMarker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIDbTxConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MagicAPIDbTxConfiguration.class, MagicAPISeataConfiguration.class))
			.withUserConfiguration(DynamicDataSourceConfiguration.class);

	@Test
	void missingDbTxMarkerDoesNotCreateDbTxBeans() {
		contextRunner.withClassLoader(new FilteredClassLoader(DbTxPluginMarker.class))
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(DbTxModule.class);
					assertThat(context).doesNotHaveBean(DbTxCoordinatorRegistry.class);
					assertThat(context).doesNotHaveBean(LocalDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(DbTxDataSourceDecorator.class);
					assertThat(context).doesNotHaveBean(SeataDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDataSourceAdapter.class);
				});
	}

	@Test
	void dbTxMarkerCreatesLocalModuleByDefault() {
		contextRunner.withClassLoader(new FilteredClassLoader(SeataPluginMarker.class))
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(DbTxProperties.class);
					assertThat(context).hasSingleBean(DbTxModule.class);
					assertThat(context).hasSingleBean(DbTxCoordinatorRegistry.class);
					assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
					assertThat(context).hasSingleBean(DbTxDataSourceDecorator.class);
					assertThat(context.getBean(MagicAPIDbTxConfiguration.class).plugin().getName())
							.isEqualTo("dbtx");
					assertThat(context.getBean(MagicAPIDbTxConfiguration.class).plugin()
							.getJavascriptFilename()).isNull();
				});
	}

	@Test
	void disabledLocalCoordinatorCannotRemainTheDefault() {
		contextRunner.withClassLoader(new FilteredClassLoader(SeataPluginMarker.class))
				.withPropertyValues("magic-api.plugin.dbtx.local-enabled=false")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(DbTxException.class)
							.hasRootCauseMessage("Coordinator is unavailable: local");
				});
	}

	@Test
	void seataCannotBeTheDefaultWithoutTheSeataPlugin() {
		contextRunner.withClassLoader(new FilteredClassLoader(SeataPluginMarker.class))
				.withPropertyValues("magic-api.plugin.dbtx.default-coordinator=seata")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(DbTxException.class)
							.hasRootCauseMessage("Coordinator is unavailable: seata");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class DynamicDataSourceConfiguration {

		@Bean
		MagicDynamicDataSource magicDynamicDataSource() {
			return new MagicDynamicDataSource();
		}
	}
}
