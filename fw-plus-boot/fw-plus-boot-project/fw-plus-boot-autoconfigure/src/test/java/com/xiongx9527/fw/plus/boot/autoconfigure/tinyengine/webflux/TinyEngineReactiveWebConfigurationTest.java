package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineServletWebConfiguration;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinyEngineReactiveWebConfigurationTest {

	private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class))
			.withUserConfiguration(ResourceServiceConfiguration.class)
			.withPropertyValues("fw.plus.tiny-engine.enabled=true");

	private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class))
			.withUserConfiguration(ResourceServiceConfiguration.class)
			.withPropertyValues("fw.plus.tiny-engine.enabled=true");

	@Test
	void servletApplicationRegistersOnlyServletTinyEngineWebConfiguration() {
		servletRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineServletWebConfiguration.class);
			assertThat(context).doesNotHaveBean(TinyEngineReactiveWebConfiguration.class);
			assertThat(context).doesNotHaveBean(TinyEngineReactiveExecutor.class);
			assertThat(context).hasSingleBean(TinyEngineResourceController.class);
		});
	}

	@Test
	void reactiveApplicationRegistersOnlyReactiveTinyEngineWebConfiguration() {
		reactiveRunner.run(context -> {
			assertThat(context).hasSingleBean(TinyEngineReactiveWebConfiguration.class);
			assertThat(context).doesNotHaveBean(TinyEngineServletWebConfiguration.class);
			assertThat(context).hasSingleBean(TinyEngineReactiveExecutor.class);
			assertThat(context).hasBean("tinyEngineBlockingScheduler");
			assertThat(context).hasSingleBean(TinyEngineResourceController.class);
		});
	}

	@Test
	void reactiveSchedulerPropertiesAreBindable() {
		reactiveRunner.withPropertyValues(
						"fw.plus.tiny-engine.webflux.scheduler.thread-count=2",
						"fw.plus.tiny-engine.webflux.scheduler.queue-capacity=16",
						"fw.plus.tiny-engine.webflux.scheduler.timeout=5s")
				.run(context -> assertThat(context).hasSingleBean(TinyEngineReactiveExecutor.class));
	}

	@Test
	void servletConfigurationMapsTinyEngineFrontendFromTinyEngineClasspathDirectory() {
		org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry =
				mock(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry.class);
		org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration registration =
				mock(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration.class);
		when(registry.addResourceHandler("/tiny-engine/**")).thenReturn(registration);

		new TinyEngineServletWebConfiguration().addResourceHandlers(registry);

		verify(registration).addResourceLocations("classpath:/META-INF/resources/tiny-engine/");
	}

	@Test
	void reactiveConfigurationMapsTinyEngineFrontendFromTinyEngineClasspathDirectory() {
		ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
		ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
		when(registry.addResourceHandler("/tiny-engine/**")).thenReturn(registration);

		new TinyEngineReactiveWebConfiguration().addResourceHandlers(registry);

		verify(registration).addResourceLocations("classpath:/tiny-engine/");
	}

	@Configuration
	static class ResourceServiceConfiguration {

		@Bean
		ResourceService resourceService() {
			return mock(ResourceService.class);
		}
	}
}
