package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.contracts;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 双栈会话归属契约测试的共享构造：把 runtime 绑到 mock 注册表上，
 * 使两栈在完全相同的会话状态（已知/未知）下展示其失效响应。
 */
final class ContractRuntime {

	private ContractRuntime() {
	}

	/**
	 * 构建 http-sse transport 的 runtime；会话持有状态由 {@code hasKnownSession} 决定。
	 */
	static McpServerRuntime sseRuntime(boolean hasKnownSession) {
		ServerSseRegistry registry = mock(ServerSseRegistry.class);
		when(registry.isOriginAllowed(null)).thenReturn(true);
		when(registry.hasSession("known-uuid")).thenReturn(hasKnownSession);
		return buildRuntime("http-sse", "/mcp", registry);
	}

	/**
	 * 构建 streamable-http transport 的 runtime；会话持有状态由 {@code hasKnownSession} 决定。
	 */
	static McpServerRuntime streamableRuntime(boolean hasKnownSession) {
		ServerStreamableHttpRegistry registry = mock(ServerStreamableHttpRegistry.class);
		when(registry.hasSession("known-session")).thenReturn(hasKnownSession);
		return buildRuntime("streamable-http", "/mcp", registry);
	}

	private static McpServerRuntime buildRuntime(String transport, String endpoint,
			com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry registry) {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties properties =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		properties.setTransport(transport);
		properties.setEndpoint(endpoint);
		return new McpServerRuntime("default", properties, mock(com.ajaxjs.mcp.server.McpServer.class), null,
				registry, null, java.util.Collections.<String>emptyList());
	}
}