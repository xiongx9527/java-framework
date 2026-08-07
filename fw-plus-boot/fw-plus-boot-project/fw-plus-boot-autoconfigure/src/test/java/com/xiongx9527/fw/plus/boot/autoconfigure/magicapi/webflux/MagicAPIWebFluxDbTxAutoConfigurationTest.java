package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.ManagedMagicDynamicDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.datasource.SeataDataSourceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIWebFluxDbTxAutoConfigurationTest {

	private final ReactiveWebApplicationContextRunner contextRunner =
			new ReactiveWebApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(MagicAPIWebFluxAutoConfiguration.class));

	@Test
	void wiresDbTxAndDecoratesRegistryDataSourcesByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(DbTxModule.class);
			assertThat(context).hasSingleBean(DbTxCoordinatorRegistry.class);
			assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
			assertThat(context).hasSingleBean(DbTxDataSourceDecorator.class);
			MagicDynamicDataSource dynamicDataSource = context.getBean(MagicDynamicDataSource.class);
			assertThat(dynamicDataSource).isInstanceOf(ManagedMagicDynamicDataSource.class);

			DataSourceMagicDynamicRegistry registry =
					context.getBean(DataSourceMagicDynamicRegistry.class);
			assertThat(registry.register(dataSourceInfo("reactive-default"))).isTrue();
			assertThat(dynamicDataSource.getDataSource("reactive-default").getDataSource())
					.isInstanceOf(DbTxAwareDataSource.class);
			dynamicDataSource.delete("reactive-default");
		});
	}

	@Test
	void disabledDbTxLeavesReactiveRegistryDataSourceUnwrapped() {
		contextRunner.withPropertyValues("magic-api.dbtx.enabled=false")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(DbTxModule.class);
					assertThat(context).doesNotHaveBean(DbTxCoordinatorRegistry.class);
					assertThat(context).doesNotHaveBean(LocalDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(DbTxDataSourceDecorator.class);

					MagicDynamicDataSource dynamicDataSource =
							context.getBean(MagicDynamicDataSource.class);
					DataSourceMagicDynamicRegistry registry =
							context.getBean(DataSourceMagicDynamicRegistry.class);
					assertThat(registry.register(dataSourceInfo("reactive-disabled"))).isTrue();
					assertThat(dynamicDataSource.getDataSource("reactive-disabled").getDataSource())
							.isNotInstanceOf(DbTxAwareDataSource.class);
					dynamicDataSource.delete("reactive-disabled");
				});
	}

	@Test
	void backsOffForCustomManagedReactiveDataSourceAndStillDecoratesRegistrations() {
		contextRunner.withUserConfiguration(CustomManagedDataSourceConfiguration.class)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(MagicDynamicDataSource.class);
					MagicDynamicDataSource dynamicDataSource =
							context.getBean(MagicDynamicDataSource.class);
					assertThat(dynamicDataSource)
							.isInstanceOf(CustomManagedMagicDynamicDataSource.class);

					DataSourceMagicDynamicRegistry registry =
							context.getBean(DataSourceMagicDynamicRegistry.class);
					assertThat(registry.register(dataSourceInfo("reactive-custom"))).isTrue();
					assertThat(dynamicDataSource.getDataSource("reactive-custom").getDataSource())
							.isInstanceOf(DbTxAwareDataSource.class);
					dynamicDataSource.delete("reactive-custom");
				});
	}

	@Test
	void keepsSeataConditionalOnPropertyAndClasspathInReactiveContext() {
		contextRunner.run(context -> assertThat(context)
				.doesNotHaveBean(SeataDbTxCoordinator.class)
				.doesNotHaveBean(SeataDataSourceAdapter.class));

		contextRunner.withPropertyValues("magic-api.dbtx.seata-enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(SeataDbTxCoordinator.class)
						.hasSingleBean(SeataDataSourceAdapter.class));

		contextRunner.withClassLoader(new FilteredClassLoader("io.seata"))
				.withPropertyValues("magic-api.dbtx.seata-enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(LocalDbTxCoordinator.class)
						.doesNotHaveBean(SeataDbTxCoordinator.class)
						.doesNotHaveBean(SeataDataSourceAdapter.class));
	}

	private static DataSourceInfo dataSourceInfo(String key) {
		DataSourceInfo info = new DataSourceInfo();
		info.setId(key + "-id");
		info.setKey(key);
		info.setName(key);
		info.setType("com.zaxxer.hikari.HikariDataSource");
		info.setUrl("jdbc:h2:mem:" + key + ";DB_CLOSE_DELAY=-1");
		info.setUsername("sa");
		info.setPassword("");
		info.setDriverClassName("org.h2.Driver");
		info.setMaxRows(100);
		return info;
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomManagedDataSourceConfiguration {

		@Bean
		MagicDynamicDataSource customManagedMagicDynamicDataSource() {
			return new CustomManagedMagicDynamicDataSource();
		}
	}

	private static final class CustomManagedMagicDynamicDataSource
			extends ManagedMagicDynamicDataSource {
	}
}
