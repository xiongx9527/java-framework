package com.xiongx9527.fw.plus.ai.mcp.transport.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import com.xiongx9527.fw.plus.ai.mcp.transport.AbstractStreamableTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 传输实现。
 *
 * <p>继承 {@link AbstractStreamableTransport} 复用会话管理和协议逻辑，
 * 将 SSE 发送通道替换为 WebSocket 发送。
 *
 * <p>WebSocket 传输不经过 HTTP 会话管理，每个连接对应一个 MCP 会话。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class McpWebSocketTransport extends AbstractStreamableTransport {

	private static final Logger logger = LoggerFactory.getLogger(McpWebSocketTransport.class);

	private volatile McpWebSocketServerTransportProvider transportProvider;
	private volatile McpAsyncServerImpl server;

	public McpWebSocketTransport(String name, String path, long sessionTimeoutMillis,
			ObjectMapper objectMapper) {
		this(name, path, sessionTimeoutMillis, objectMapper, null);
	}

	public McpWebSocketTransport(String name, String path, long sessionTimeoutMillis,
			ObjectMapper objectMapper, com.xiongx9527.fw.plus.ai.mcp.transport.session.McpSessionStore sessionStore) {
		super(name, path, sessionTimeoutMillis, objectMapper, sessionStore);
	}

	// ==================== McpTransportLifecycle ====================

	@Override
	public void bindServer(McpAsyncServerImpl server) {
		this.server = server;
		super.bindServer(server);
	}

	/**
	 * 设置 WebSocket 传输提供者（端点持有实际的 WebSocket 连接）
	 */
	public void setTransportProvider(McpWebSocketServerTransportProvider transportProvider) {
		this.transportProvider = transportProvider;
	}

	// ==================== WebSocket 消息处理 ====================

	/**
	 * 处理客户端通过 WebSocket 发来的 JSON-RPC 请求
	 *
	 * @param sessionId 会话 ID（首次连接时可为 null）
	 * @param body      JSON-RPC 请求体
	 * @return 响应 JSON，无响应时返回 null
	 */
	public String handleMessage(String sessionId, String body) {
		McpJsonRpcRequest request = parseRequest(body);
		if (request == null) {
			return null;
		}
		McpAsyncServerImpl target = this.server;
		if (target == null) {
			return null;
		}
		McpJsonRpcResponse response = target.handleRequest(request, sessionId);
		if (response == null) {
			return null;
		}
		return serializeResponse(response);
	}

	/**
	 * 通过 WebSocket 发送通知到客户端
	 */
	public void sendNotification(String method, Object params) {
		String payload = buildNotification(method, params);
		McpWebSocketServerTransportProvider provider = this.transportProvider;
		if (provider != null) {
			try {
				provider.sendRaw(payload);
			} catch (Exception e) {
				logger.debug("Failed to send WebSocket notification [{}]", method, e);
			}
		}
	}

	// ==================== 内部方法 ====================

	private String serializeResponse(McpJsonRpcResponse response) {
		try {
			return getObjectMapper().writeValueAsString(response);
		} catch (Exception e) {
			logger.error("Failed to serialize JSON-RPC response", e);
			return null;
		}
	}
}
