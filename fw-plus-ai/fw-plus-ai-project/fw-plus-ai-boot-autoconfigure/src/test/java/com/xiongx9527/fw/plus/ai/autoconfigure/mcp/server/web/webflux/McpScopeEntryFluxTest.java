package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
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

		assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldRejectSseMessageWhenScopesAreEmpty() throws Exception {
		McpServerRuntime runtime = sseRuntime(principal(), true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}")).block();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
	void shouldBypassScopeCheckForExplicitPermitAll() throws Exception {
		// 显式 permit-all 主体没有 scopes，开启裁剪时也放行进内核（spec S4）
		McpServerRuntime runtime = sseRuntime(com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal.PERMIT_ALL, true);
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, Schedulers.immediate());

		ServerResponse response = invoke(handler, "handleMessage",
				messageRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}")).block();

		assertThat(response.statusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
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

		assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
		when(runtime.getRegistry()).thenReturn((com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry) registry);
		when(runtime.hasSession("known-uuid")).thenReturn(true);
		when(runtime.hasSession("known-session")).thenReturn(true);
		when(runtime.getAuthService()).thenReturn(new AuthService() {
			@Override
			public com.xiongx9527.fw.plus.auth.core.AuthPrincipal authenticate(com.xiongx9527.fw.plus.auth.core.AuthRequest request) {
				if (principal == com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal.PERMIT_ALL) {
					return com.xiongx9527.fw.plus.auth.core.AuthPrincipal.PERMIT_ALL;
				}
				return new com.xiongx9527.fw.plus.auth.core.AuthPrincipal(principal.getSubject(), principal.getScopes());
			}
		});
		return runtime;
	}

	private McpPrincipal principal(String... scopes) {
		Set<String> set = new HashSet<>();
		Collections.addAll(set, scopes);
		return new McpPrincipal("tester", set);
	}

	// MockServerRequest(5.3) 的 bodyToMono 直接返回 body 字段，body 必须是 Mono；
// Builder#body 本身即返回 MockServerRequest，无需再 build()
	private ServerRequest messageRequest(String body) {
		return MockServerRequest.builder()
				.method(org.springframework.http.HttpMethod.POST)
				.uri(java.net.URI.create("/mcp"))
				.pathVariable("uuid", "known-uuid")
				.header("Origin", "")
				.body(Mono.just(body));
	}

	private ServerRequest postRequest(String body) {
		return MockServerRequest.builder()
				.method(org.springframework.http.HttpMethod.POST)
				.uri(java.net.URI.create("/mcp"))
				.header("Mcp-Session-Id", "known-session")
				.body(Mono.just(body));
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