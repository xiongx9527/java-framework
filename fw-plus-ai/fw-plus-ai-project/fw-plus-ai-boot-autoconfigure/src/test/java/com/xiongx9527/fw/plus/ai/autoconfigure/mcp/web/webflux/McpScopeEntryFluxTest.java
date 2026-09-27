package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * scope 裁剪入口契约（webflux）：开启裁剪后，缺目标能力 scope 的请求在入口即被拒，
 * 不进内核；具备 scope 与缺省关闭（不裁剪）时行为不变。
 *
 * <p>覆盖默认传输 {@code http-sse} 的 legacy SSE 消息入口——该入口曾因未透传 body
 * 导致裁剪空转（修复前仅 streamable-http 的 POST 生效）。</p>
 */
class McpScopeEntryFluxTest {

	@Test
	void shouldRejectSseMessageWhenCapabilityScopeMissing() throws Exception {
		McpServerRuntime runtime = sseRuntime(principal("tools"), true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}")).block();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldEnterKernelOnSseMessageWhenCapabilityScopePresent() throws Exception {
		McpServerRuntime runtime = sseRuntime(principal("tools", "prompts"), true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}")).block();

		verify((ServerSseRegistry) runtime.getRegistry()).handle(any(), any());
	}

	@Test
	void shouldNotCutScopeWhenCheckDisabledByDefault() throws Exception {
		McpServerRuntime runtime = sseRuntime(principal("tools"), false);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}")).block();

		verify((ServerSseRegistry) runtime.getRegistry()).handle(any(), any());
	}

	@Test
	void shouldKeepNonCapabilityOperationsUnaffected() throws Exception {
		McpServerRuntime runtime = sseRuntime(principal("tools"), true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}")).block();

		verify((ServerSseRegistry) runtime.getRegistry()).handle(any(), any());
	}

	@Test
	void shouldRejectStreamablePostWhenCapabilityScopeMissing() throws Exception {
		McpServerRuntime runtime = streamableRuntime(principal("tools"), true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		when(registry.post(any(), anyMap())).thenReturn(com.ajaxjs.mcp.server.model.HttpResult.json(200, null));
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "post",
				postRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/read\"}")).block();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(registry, never()).post(any(), anyMap());
	}

	private McpServerRuntime sseRuntime(McpPrincipal principal, boolean scopeCheckEnabled) {
		ServerSseRegistry registry = mock(ServerSseRegistry.class);
		when(registry.isOriginAllowed(any())).thenReturn(true);
		when(registry.hasSession("known-uuid")).thenReturn(true);
		return runtime("http-sse", registry, principal, scopeCheckEnabled);
	}

	private McpServerRuntime streamableRuntime(McpPrincipal principal, boolean scopeCheckEnabled) {
		ServerStreamableHttpRegistry registry = mock(ServerStreamableHttpRegistry.class);
		when(registry.hasSession("known-session")).thenReturn(true);
		return runtime("streamable-http", registry, principal, scopeCheckEnabled);
	}

	private McpServerRuntime runtime(String transport, Object registry, McpPrincipal principal,
			boolean scopeCheckEnabled) {
		McpProperties properties = new McpProperties();
		properties.setTransport(transport);
		properties.setEndpoint("/mcp");
		properties.setScopeCheckEnabled(scopeCheckEnabled);
		McpServerRuntime runtime = mock(McpServerRuntime.class);
		when(runtime.getProperties()).thenReturn(properties);
		when(runtime.getTransport()).thenReturn(transport);
		when(runtime.getEndpoint()).thenReturn("/mcp");
		when(runtime.getKey()).thenReturn("default");
		when(runtime.getRegistry()).thenReturn((com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry) registry);
		when(runtime.hasSession("known-uuid")).thenReturn(true);
		when(runtime.hasSession("known-session")).thenReturn(true);
		when(runtime.getAuthService()).thenReturn(new McpAuthService() {
			@Override
			public McpPrincipal authenticate(McpRequest request) {
				return principal;
			}
		});
		return runtime;
	}

	private McpPrincipal principal(String... scopes) {
		Set<String> set = new HashSet<>();
		Collections.addAll(set, scopes);
		return new McpPrincipal("tester", set);
	}

	private ServerRequest postRequest(String body) {
		ServerRequest.Headers headers = headers("known-session");
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);
		when(request.bodyToMono(String.class)).thenReturn(Mono.just(body));
		return request;
	}

	private ServerRequest messageRequest(String body) {
		ServerRequest.Headers headers = headers(null);
		ServerRequest request = mock(ServerRequest.class);
		when(request.headers()).thenReturn(headers);
		when(request.pathVariable("uuid")).thenReturn("known-uuid");
		when(request.bodyToMono(String.class)).thenReturn(Mono.just(body));
		return request;
	}

	private ServerRequest.Headers headers(String session) {
		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		when(headers.firstHeader("Mcp-Session-Id")).thenReturn(session);
		when(headers.firstHeader("Origin")).thenReturn(null);
		when(headers.asHttpHeaders()).thenReturn(new HttpHeaders());
		return headers;
	}

	private Mono<ServerResponse> invoke(McpFluxTransportHandler handler, String methodName, ServerRequest request)
			throws Exception {
		Method method = McpFluxTransportHandler.class.getDeclaredMethod(methodName, ServerRequest.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		Mono<ServerResponse> result = (Mono<ServerResponse>) method.invoke(handler, request);
		return result;
	}
}
