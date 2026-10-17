package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.DbTxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter.MagicAPIDbTxConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.DefaultSeataTransactionFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.datasource.SeataDataSourceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicAPISeataConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MagicAPIDbTxConfiguration.class, MagicAPISeataConfiguration.class))
			.withUserConfiguration(DynamicDataSourceConfiguration.class);

	@Test
	void disabledSeataLeavesLocalCoordinatorAvailable() {
		contextRunner.withPropertyValues("magic-api.dbtx.seata-enabled=false")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(DbTxModule.class);
					assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDataSourceAdapter.class);
					assertThat(context.getBean(DbTxCoordinatorRegistry.class).contains("seata"))
							.isFalse();
				});
	}

	@Test
	void enabledSeataCreatesCoordinatorAndAdapterAlongsideLocal() {
		contextRunner.withPropertyValues("magic-api.dbtx.seata-enabled=true")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(DefaultSeataTransactionFactory.class);
					assertThat(context).hasSingleBean(SeataDbTxCoordinator.class);
					assertThat(context).hasSingleBean(SeataDataSourceAdapter.class);
					assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
					assertThat(context.getBean(DbTxCoordinatorRegistry.class).contains("local")).isTrue();
					assertThat(context.getBean(DbTxCoordinatorRegistry.class).contains("seata")).isTrue();
				});
	}

	@Test
	void missingSeataClassesKeepsLocalUsableAndRejectsExplicitSeata() {
		contextRunner.withClassLoader(new FilteredClassLoader("org.apache.seata"))
				.withPropertyValues("magic-api.dbtx.seata-enabled=true")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(LocalDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDbTxCoordinator.class);
					assertThat(context).doesNotHaveBean(SeataDataSourceAdapter.class);

					DbTxModule module = context.getBean(DbTxModule.class);
					assertThatThrownBy(() -> module.transaction("seata",
							Collections.singletonList("primary"), ignored -> null))
							.isInstanceOfSatisfying(DbTxException.class, exception -> {
								assertThat(exception.getDbTxCode())
										.isEqualTo(DbTxErrorCode.DBTX_COORDINATOR_UNAVAILABLE);
								assertThat(exception).hasMessage("Coordinator is unavailable: seata");
							});
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
