package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webmvc;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
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
 * scope 裁剪入口契约（webmvc）：开启裁剪后，缺目标能力 scope 的请求在入口即被拒（403），
 * 不进内核；具备 scope 与缺省关闭（不裁剪）时行为不变。与 {@code McpScopeEntryFluxTest} 对偶。
 */
class McpScopeEntryMvcTest {

	@Test
	void shouldRejectSseMessageWhenCapabilityScopeMissing() throws IOException {
		McpServerRuntime runtime = sseRuntime(principal("tools"), true);
		McpSseController controller = new McpSseController(runtime);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}", "known-uuid",
				new MockHttpServletRequest(), response);

		assertThat(response.getStatus()).isEqualTo(403);
		verify((ServerSseRegistry) runtime.getRegistry(), never()).handle(any(), any());
	}

	@Test
	void shouldEnterKernelOnSseMessageWhenCapabilityScopePresent() throws IOException {
		McpServerRuntime runtime = sseRuntime(principal("tools", "prompts"), true);
		McpSseController controller = new McpSseController(runtime);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}", "known-uuid",
				new MockHttpServletRequest(), response);

		verify((ServerSseRegistry) runtime.getRegistry()).handle(any(), any());
	}

	@Test
	void shouldNotCutScopeWhenCheckDisabledByDefault() throws IOException {
		McpServerRuntime runtime = sseRuntime(principal("tools"), false);
		McpSseController controller = new McpSseController(runtime);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"prompts/get\"}", "known-uuid",
				new MockHttpServletRequest(), response);

		verify((ServerSseRegistry) runtime.getRegistry()).handle(any(), any());
	}

	@Test
	void shouldRejectStreamablePostWhenCapabilityScopeMissing() throws IOException {
		McpServerRuntime runtime = streamableRuntime(principal("tools"), true);
		ServerStreamableHttpRegistry registry = (ServerStreamableHttpRegistry) runtime.getRegistry();
		when(registry.post(any(), anyMap())).thenReturn(com.ajaxjs.mcp.server.model.HttpResult.json(200, null));
		McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Mcp-Session-Id", "known-session");

		controller.post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/read\"}", request,
				new MockHttpServletResponse());

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
}
