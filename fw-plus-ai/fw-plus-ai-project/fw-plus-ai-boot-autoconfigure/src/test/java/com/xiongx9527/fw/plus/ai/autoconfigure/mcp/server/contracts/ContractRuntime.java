package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.PermitAllAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 双栈会话归属契约测试的共享构造：把 runtime 绑到 mock 注册表上，
 * 使两栈在完全相同的会话状态（已知/未知）下展示其失效响应。
 */
public final class ContractRuntime {

	private ContractRuntime() {
	}

	/**
	 * 构建 http-sse transport 的 runtime；会话持有状态由 {@code hasKnownSession} 决定。
	 */
	public static McpServerRuntime sseRuntime(boolean hasKnownSession) {
		return sseRuntime(hasKnownSession, new PermitAllAuthService());
	}

	/**
	 * 指定认证服务的 http-sse runtime。
	 */
	public static McpServerRuntime sseRuntime(boolean hasKnownSession,
			com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService authService) {
		ServerSseRegistry registry = mock(ServerSseRegistry.class);
		when(registry.isOriginAllowed(null)).thenReturn(true);
		when(registry.hasSession("known-uuid")).thenReturn(hasKnownSession);
		return buildRuntime("http-sse", "/mcp", registry, authService);
	}

	/**
	 * 构建 streamable-http transport 的 runtime；会话持有状态由 {@code hasKnownSession} 决定。
	 */
	public static McpServerRuntime streamableRuntime(boolean hasKnownSession) {
		return streamableRuntime(hasKnownSession, new PermitAllAuthService());
	}

	/**
	 * 指定认证服务的 streamable-http runtime。
	 */
	public static McpServerRuntime streamableRuntime(boolean hasKnownSession,
			com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService authService) {
		ServerStreamableHttpRegistry registry = mock(ServerStreamableHttpRegistry.class);
		when(registry.hasSession("known-session")).thenReturn(hasKnownSession);
		return buildRuntime("streamable-http", "/mcp", registry, authService);
	}

	private static McpServerRuntime buildRuntime(String transport, String endpoint,
			com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry registry,
			com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService authService) {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties properties =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties();
		properties.setTransport(transport);
		properties.setEndpoint(endpoint);
		return new McpServerRuntime("default", properties, mock(com.ajaxjs.mcp.server.McpServer.class), null,
				registry, authService, java.util.Collections.<String>emptyList());
	}
}