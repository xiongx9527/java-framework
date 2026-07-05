package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionState;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpLifecycleHandlersTest {

	@Test
	void shouldInitializeSessionAndReturnCapabilities() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpLifecycleHandlers handlers = new McpLifecycleHandlers(sessionService);
		McpRequestContext context = new McpRequestContext(new McpPrincipal("user-1", Collections.emptySet()));
		Map<String, Object> params = new HashMap<>();
		params.put("protocolVersion", "2025-06-18");
		params.put("capabilities", Collections.singletonMap("tools", Collections.emptyMap()));

		Map<String, Object> result = handlers.initialize(context, params);

		assertThat(result.get("protocolVersion")).isEqualTo("2025-06-18");
		assertThat(result.get("capabilities")).isInstanceOf(Map.class);
		assertThat(context.getSessionId()).isNotBlank();
		assertThat(sessionService.requireSession(context.getSessionId()).getState()).isEqualTo(McpSessionState.INITIALIZED);
	}

	@Test
	void shouldMarkInitializedSessionReady() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpLifecycleHandlers handlers = new McpLifecycleHandlers(sessionService);
		McpRequestContext context = new McpRequestContext(new McpPrincipal("user-1", Collections.emptySet()));
		Map<String, Object> result = handlers.initialize(context, Collections.emptyMap());

		handlers.initialized(context, null);

		assertThat(result).containsKey("serverInfo");
		assertThat(sessionService.requireSession(context.getSessionId()).getState()).isEqualTo(McpSessionState.READY);
	}

	@Test
	void shouldReturnEmptyPingResult() {
		McpLifecycleHandlers handlers = new McpLifecycleHandlers(new McpSessionService(1800));

		assertThat(handlers.ping(new McpRequestContext(), null)).isEqualTo(Collections.emptyMap());
	}

	@Test
	void shouldTrackCancellationAndProgressNotifications() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpLifecycleHandlers handlers = new McpLifecycleHandlers(sessionService);
		com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession session =
				sessionService.createSession(null, Collections.emptyMap(), "2025-06-18");
		session.getClientRequests().put("request-1", Collections.singletonMap("method", "tools/call"));
		McpRequestContext context = new McpRequestContext();
		context.setSessionId(session.getId());
		Map<String, Object> cancelled = new HashMap<>();
		cancelled.put("requestId", "request-1");

		handlers.cancelled(context, cancelled);
		Map<String, Object> progress = new HashMap<>();
		progress.put("progressToken", "token-1");
		progress.put("progress", 1);

		assertThat(session.getCancelledRequests()).contains("request-1");
		assertThat(handlers.progress(context, progress)).isEqualTo(Collections.emptyMap());
	}

	@Test
	void shouldRejectFeatureBeforeReady() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpLifecycleHandlers handlers = new McpLifecycleHandlers(sessionService);
		McpRequestContext context = new McpRequestContext(new McpPrincipal("user-1", Collections.emptySet()));
		handlers.initialize(context, Collections.emptyMap());

		assertThatThrownBy(() -> handlers.requireReady(context))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Session is not ready");
	}
}
