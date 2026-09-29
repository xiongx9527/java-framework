package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts.ContractRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebFlux 装配级验证：RouterFunction bean 在容器装配后，运行期增删 runtime 仍能反映到路由上
 * （WebFlux 启动期只读一次 bean，动态生效依赖内部委托）。
 */
class McpWebfluxBindingIntegrationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void shouldServeRoutesConfiguredAtStartup() {
		contextRunner.run(context -> {
			RouterFunction<ServerResponse> router = context.getBean(RouterFunction.class);

			assertThat(route(router, "/mcp/sse")).isTrue();
			assertThat(route(router, "/mcp/absent/sse")).isFalse();
		});
	}

	@Test
	void shouldReflectRuntimeAddedAfterStartup() {
		contextRunner.run(context -> {
			RouterFunction<ServerResponse> router = context.getBean(RouterFunction.class);
			McpFluxRouteRegistry registry = context.getBean(McpFluxRouteRegistry.class);
			assertThat(route(router, "/mcp2/sse")).isFalse();

			registry.bind(runtime("report", "/mcp2"));

			assertThat(route(router, "/mcp2/sse")).isTrue();
		});
	}

	@Test
	void shouldStopServingRouteAfterUnbind() {
		contextRunner.run(context -> {
			RouterFunction<ServerResponse> router = context.getBean(RouterFunction.class);
			McpFluxRouteRegistry registry = context.getBean(McpFluxRouteRegistry.class);
			assertThat(route(router, "/mcp/sse")).isTrue();

			registry.unbind(runtime("default", "/mcp"));

			assertThat(route(router, "/mcp/sse")).isFalse();
		});
	}

	@Test
	void shouldReturnEmptyInsteadOfNullWhenNoRouteMatches() {
		contextRunner.run(context -> {
			RouterFunction<ServerResponse> router = context.getBean(RouterFunction.class);

			assertThat(route(router, "/nope")).isFalse();
		});
	}

	private boolean route(RouterFunction<ServerResponse> router, String path) {
		ServerRequest request = ServerRequest.create(
				MockServerWebExchange.from(MockServerHttpRequest.get(path)), Collections.emptyList());
		return router.route(request).block() != null;
	}

	@SuppressWarnings("unused")
	private McpServerRuntime runtime(String key, String endpoint) {
		McpServerRuntime runtime = mock(McpServerRuntime.class);
		when(runtime.getKey()).thenReturn(key);
		when(runtime.getEndpoint()).thenReturn(endpoint);
		when(runtime.getTransport()).thenReturn("http-sse");
		return runtime;
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public McpProperties mcpProperties() {
			McpProperties properties = new McpProperties();
			properties.setTransport("http-sse");
			properties.setEndpoint("/mcp");
			return properties;
		}

		@Bean
		public List<McpServerRuntime> runtimes() {
			return Collections.singletonList(runtime("default", "/mcp"));
		}

		@Bean(destroyMethod = "shutdown")
		public ExecutorService mcpThreadPool() {
			return Executors.newSingleThreadExecutor();
		}

		@Bean
		public McpFluxRouteRegistry mcpFluxRouteRegistry(List<McpServerRuntime> runtimes,
				ExecutorService mcpThreadPool) {
			return new McpFluxRouteRegistry(runtimes, mcpThreadPool);
		}

		@Bean
		public RouterFunction<ServerResponse> mcpRouterFunction(McpFluxRouteRegistry registry) {
			return registry.router();
		}

		private McpServerRuntime runtime(String key, String endpoint) {
			McpServerRuntime runtime = mock(McpServerRuntime.class);
			when(runtime.getKey()).thenReturn(key);
			when(runtime.getEndpoint()).thenReturn(endpoint);
			when(runtime.getTransport()).thenReturn("http-sse");
			return runtime;
		}
	}
}
