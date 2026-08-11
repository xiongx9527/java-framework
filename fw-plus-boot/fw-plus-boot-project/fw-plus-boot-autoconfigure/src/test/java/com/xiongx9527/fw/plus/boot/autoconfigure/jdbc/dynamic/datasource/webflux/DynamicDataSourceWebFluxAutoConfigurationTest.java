package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicDataSourceWebFluxAutoConfigurationTest {

	private final ReactiveWebApplicationContextRunner reactiveRunner =
			new ReactiveWebApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(
							DynamicDataSourceWebFluxAutoConfiguration.class));

	@Test
	void reactiveApplicationRegistersBridgeBeans() {
		reactiveRunner.run(context -> {
			assertThat(context).hasSingleBean(DsWebFluxContextHook.class);
			assertThat(context).hasSingleBean(DsWebFluxRequestContextFilter.class);
		});
	}

	@Test
	void servletApplicationDoesNotRegisterBridgeBeans() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						DynamicDataSourceWebFluxAutoConfiguration.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(DsWebFluxContextHook.class);
					assertThat(context).doesNotHaveBean(DsWebFluxRequestContextFilter.class);
				});
	}

	@Test
	void nonWebApplicationDoesNotRegisterBridgeBeans() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						DynamicDataSourceWebFluxAutoConfiguration.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(DsWebFluxContextHook.class);
					assertThat(context).doesNotHaveBean(DsWebFluxRequestContextFilter.class);
				});
	}

}
