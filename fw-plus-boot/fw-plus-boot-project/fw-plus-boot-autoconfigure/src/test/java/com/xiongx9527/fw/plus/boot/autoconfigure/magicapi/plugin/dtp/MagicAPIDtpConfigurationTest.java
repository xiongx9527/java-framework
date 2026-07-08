package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service.DtpDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter.MagicAPIDtpConfiguration;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIDtpConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MagicAPIDtpConfiguration.class));

	@Test
	void backsOffWhenPluginDisabled() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=false")
				.run(context -> assertThat(context)
						.doesNotHaveBean(MagicDynamicDtpDataSource.class)
						.doesNotHaveBean(DtpExecutor.class)
						.doesNotHaveBean(DtpModule.class));
	}

	@Test
	void createsDefaultBeansWhenEnabled() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=true")
				.run(context -> assertThat(context)
						.hasSingleBean(MagicDynamicDtpDataSource.class)
						.hasSingleBean(DtpExecutor.class)
						.hasSingleBean(DtpModule.class)
						.hasSingleBean(DtpDataSourceInfoMagicResourceStorage.class)
						.hasSingleBean(DtpDataSourceMagicDynamicRegistry.class));
	}

	@Test
	void backsOffForUserBeans() {
		contextRunner
				.withPropertyValues("magic-api.plugin.dtp.enabled=true")
				.withUserConfiguration(UserDtpConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(MagicDynamicDtpDataSource.class);
					assertThat(context).hasSingleBean(DtpExecutor.class);
					assertThat(context).hasSingleBean(DtpModule.class);
					assertThat(context).hasSingleBean(DtpDataSourceInfoMagicResourceStorage.class);
					assertThat(context).hasSingleBean(DtpDataSourceMagicDynamicRegistry.class);
					assertThat(context.getBean(MagicDynamicDtpDataSource.class))
							.isSameAs(context.getBean("customDynamicDtpDataSource"));
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserDtpConfiguration {

		@Bean
		MagicDynamicDtpDataSource customDynamicDtpDataSource() {
			return new MagicDynamicDtpDataSource();
		}

		@Bean
		DtpExecutor customDtpExecutor() {
			return new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS,
					new LinkedBlockingQueue<>(1));
		}

		@Bean
		DtpModule customDtpModule(MagicDynamicDtpDataSource dataSource, DtpExecutor executor) {
			return new DtpModule(dataSource, executor);
		}

		@Bean
		DtpDataSourceInfoMagicResourceStorage customStorage() {
			return new DtpDataSourceInfoMagicResourceStorage();
		}

		@Bean
		DtpDataSourceMagicDynamicRegistry customRegistry() {
			return mock(DtpDataSourceMagicDynamicRegistry.class);
		}
	}
}
