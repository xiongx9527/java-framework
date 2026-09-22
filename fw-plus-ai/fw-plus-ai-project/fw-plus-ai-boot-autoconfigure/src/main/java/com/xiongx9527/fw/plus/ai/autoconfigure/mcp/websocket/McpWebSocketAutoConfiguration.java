package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.websocket;

import com.xiongx9527.fw.plus.ai.mcp.transport.websocket.McpWebSocketEndpoint;
import com.xiongx9527.fw.plus.ai.mcp.transport.websocket.McpWebSocketTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * Spring MVC WebSocket 自动配置。
 *
 * <p>当 {@code fw-plus.ai.mcp.websocket-enabled=true} 时装配，
 * 为每个 enabled MCP 实例注册 WebSocket 端点。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "websocket-enabled", havingValue = "true")
public class McpWebSocketAutoConfiguration implements WebSocketConfigurer {

	private final List<McpWebSocketTransport> transports;

	public McpWebSocketAutoConfiguration(List<McpWebSocketTransport> transports) {
		this.transports = transports;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		for (McpWebSocketTransport transport : transports) {
			String path = transport.getPath();
			registry.addHandler(new McpWebSocketEndpoint(transport), path);
		}
	}
}
