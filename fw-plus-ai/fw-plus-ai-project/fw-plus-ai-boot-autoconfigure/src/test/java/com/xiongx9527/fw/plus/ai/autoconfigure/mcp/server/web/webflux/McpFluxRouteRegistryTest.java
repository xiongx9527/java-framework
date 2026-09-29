package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpFluxRouteRegistryTest {

	private final McpServerRuntime runtime = runtime("report", "/mcp/report");

	@Test
	void shouldRouteRegisteredEndpoint() {
		McpFluxRouteRegistry registry = new McpFluxRouteRegistry(Collections.singletonList(runtime),
				Executors.newSingleThreadExecutor());
		RouterFunction<ServerResponse> router = registry.router();

		assertThat(route(router, "/mcp/report/sse")).isTrue();
	}

	@Test
	void shouldBindEndpointAtRuntime() {
		McpFluxRouteRegistry registry = new McpFluxRouteRegistry(Collections.<McpServerRuntime>emptyList(),
				Executors.newSingleThreadExecutor());
		RouterFunction<ServerResponse> router = registry.router();
		assertThat(route(router, "/mcp/report/sse")).isFalse();

		registry.bind(runtime("report", "/mcp/report"));

		assertThat(route(router, "/mcp/report/sse")).isTrue();
	}

	@Test
	void shouldUnbindEndpointAtRuntime() {
		McpFluxRouteRegistry registry = new McpFluxRouteRegistry(Collections.singletonList(runtime),
				Executors.newSingleThreadExecutor());
		RouterFunction<ServerResponse> router = registry.router();

		registry.unbind(runtime);

		assertThat(route(router, "/mcp/report/sse")).isFalse();
	}

	private McpServerRuntime runtime() {
		return runtime;
	}

	private boolean route(RouterFunction<ServerResponse> router, String path) {
		ServerRequest request = ServerRequest.create(
				MockServerWebExchange.from(MockServerHttpRequest.get(path)), Collections.emptyList());
		Mono<HandlerFunction<ServerResponse>> result = router.route(request);
		return result != null && result.block() != null;
	}

	private McpServerRuntime runtime(String key, String endpoint) {
		McpServerRuntime runtime = mock(McpServerRuntime.class);
		when(runtime.getKey()).thenReturn(key);
		when(runtime.getEndpoint()).thenReturn(endpoint);
		when(runtime.getTransport()).thenReturn("http-sse");
		return runtime;
	}
}
