package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * WebFlux 栈自动装配契约：响应式应用注册端点、Servlet 应用不注册、关闭开关生效（Scenario A11/A14）。
 *
 * <p>登记项收敛后只登记唯一入口 {@link WsAutoConfiguration}，webflux 装配由其 {@code @Import} 收编。</p>
 *
 * @author zzy
 */
class WsWebFluxAutoConfigurationTest {

	@Test
	void reactiveApplicationShouldRegisterReactiveHandlerOnConfiguredPath() {
		new ReactiveWebApplicationContextRunner()
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withPropertyValues(
						"fw.plus.ws.path=/ws/reactive")
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
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withUserConfiguration(BasePathConfiguration.class)
				.withPropertyValues(
						"fw.plus.ws.path=/ws/api")
				.run(context -> assertThat(context.getBean(SimpleUrlHandlerMapping.class).getUrlMap())
						.containsKey("/gateway/ws/api"));
	}

	/**
	 * A4：追加路径与多个基础路径提供者在响应式栈展开为同一组端点。
	 */
	@Test
	void reactiveApplicationShouldExpandEveryPathAgainstEveryBasePath() {
		new ReactiveWebApplicationContextRunner()
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withUserConfiguration(MultiBasePathConfiguration.class)
				.withPropertyValues(
						"fw.plus.ws.path=/ws/api",
						"fw.plus.ws.paths=/ws/api2")
				.run(context -> {
					SimpleUrlHandlerMapping mapping = context.getBean(SimpleUrlHandlerMapping.class);
					assertThat(mapping.getUrlMap()).containsKeys(
							"/gw-a/ws/api", "/gw-a/ws/api2", "/gw-b/ws/api", "/gw-b/ws/api2");
				});
	}

	/**
	 * A5：主路径显式置空时没有本地 {@code /ws/api} 回落，响应式栈与 webmvc 口径一致——
	 * 只在基础路径后追加一个连接斜杠，与拆分前 webmvc 的表现相同。
	 */
	@Test
	void reactiveBlankPrimaryPathShouldFollowBasePathOnly() {
		new ReactiveWebApplicationContextRunner()
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withUserConfiguration(BasePathConfiguration.class)
				.withPropertyValues(
						"fw.plus.ws.path=")
				.run(context -> assertThat(context.getBean(SimpleUrlHandlerMapping.class).getUrlMap())
						.containsOnlyKeys("/gateway/"));
	}

	/**
	 * A4：宿主自带的 {@link SimpleUrlHandlerMapping} 与地基映射器互不覆盖，由 order 决定先后。
	 */
	@Test
	void hostSuppliedMappingShouldCoexistWithFoundationMapping() {
		new ReactiveWebApplicationContextRunner()
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withUserConfiguration(BasePathConfiguration.class, HostMappingConfiguration.class)
				.withPropertyValues(
						"fw.plus.ws.path=/ws/api")
				.run(context -> {
					Map<String, SimpleUrlHandlerMapping> mappings = context.getBeansOfType(SimpleUrlHandlerMapping.class);
					assertThat(mappings).hasSize(2);

					SimpleUrlHandlerMapping foundation = mappings.get("wsReactiveHandlerMapping");
					SimpleUrlHandlerMapping host = mappings.get("hostHandlerMapping");
					assertThat(foundation.getUrlMap()).containsOnlyKeys("/gateway/ws/api");
					assertThat(host.getUrlMap()).containsOnlyKeys("/host/echo");
					assertThat(foundation.getOrder()).isLessThan(host.getOrder());
				});
	}

	@Test
	void reactiveEndpointShouldBeAbsentWhenFeatureDisabled() {
		new ReactiveWebApplicationContextRunner()
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.withPropertyValues(
						"fw.plus.ws.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(ReactiveWsHandler.class));
	}

	@Test
	void servletApplicationShouldNotRegisterReactiveHandler() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
				.withPropertyValues("fw.plus.ws.enabled=true")
				.run(context -> assertThat(context).doesNotHaveBean(ReactiveWsHandler.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BasePathConfiguration {

		@Bean
		WsBasePathProvider basePathProvider() {
			return () -> "/gateway";
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MultiBasePathConfiguration {

		@Bean
		WsBasePathProvider gatewayProvider() {
			return () -> "/gw-a";
		}

		@Bean
		WsBasePathProvider bffProvider() {
			return () -> "/gw-b";
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class HostMappingConfiguration {

		@Bean
		SimpleUrlHandlerMapping hostHandlerMapping() {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			Map<String, WebSocketHandler> urlMap = new HashMap<>();
			urlMap.put("/host/echo", mock(WebSocketHandler.class));
			mapping.setUrlMap(urlMap);
			return mapping;
		}
	}
}
