package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebFlux 栈自动装配契约：响应式应用注册端点、Servlet 应用不注册、关闭开关生效（Scenario A11/A14）。
 *
 * @author zzy
 */
class WsWebFluxAutoConfigurationTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Test
	void reactiveApplicationShouldRegisterReactiveHandlerOnConfiguredPath() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WsWebFluxAutoConfiguration.class))
				.withPropertyValues(
						"fw.plus.ws.token.secret=" + SECRET,
						"fw.plus.ws.websocket.path=/ws/reactive")
				.run(context -> {
					assertThat(context).hasSingleBean(ReactiveWsHandler.class);
					SimpleUrlHandlerMapping mapping = context.getBean(SimpleUrlHandlerMapping.class);
					assertThat(mapping.getUrlMap()).containsKey("/ws/reactive");
					assertThat(mapping.getUrlMap().get("/ws/reactive")).isInstanceOf(ReactiveWsHandler.class);
				});
	}

	@Test
	void reactiveEndpointShouldRespectBasePathProvider() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WsWebFluxAutoConfiguration.class))
				.withUserConfiguration(BasePathConfiguration.class)
				.withPropertyValues(
						"fw.plus.ws.token.secret=" + SECRET,
						"fw.plus.ws.websocket.path=/ws/api")
				.run(context -> assertThat(context.getBean(SimpleUrlHandlerMapping.class).getUrlMap())
						.containsKey("/gateway/ws/api"));
	}

	@Test
	void reactiveEndpointShouldBeAbsentWhenFeatureDisabled() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WsWebFluxAutoConfiguration.class))
				.withPropertyValues(
						"fw.plus.ws.enabled=false",
						"fw.plus.ws.token.secret=" + SECRET)
				.run(context -> assertThat(context).doesNotHaveBean(ReactiveWsHandler.class));
	}

	@Test
	void servletApplicationShouldNotRegisterReactiveHandler() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(WsWebFluxAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.token.secret=" + SECRET)
				.run(context -> assertThat(context).doesNotHaveBean(ReactiveWsHandler.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BasePathConfiguration {

		@Bean
		WsBasePathProvider basePathProvider() {
			return () -> "/gateway";
		}
	}
}
