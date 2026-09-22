package com.xiongx9527.fw.plus.ai.mcp.transport.websocket;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

/**
 * WebSocket 传输提供者接口。
 *
 * <p>定义 WebSocket 会话级别的发送能力，供 {@link McpWebSocketTransport} 委托使用。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public interface McpWebSocketServerTransportProvider {

	/**
	 * 向客户端发送 JSON-RPC 响应
	 *
	 * @param response 响应对象
	 * @throws Exception 发送失败
	 */
	void sendResponse(McpJsonRpcResponse response) throws Exception;

	/**
	 * 发送原始 JSON 文本（用于通知等非响应消息）
	 *
	 * @param json JSON 文本
	 * @throws Exception 发送失败
	 */
	void sendRaw(String json) throws Exception;

	/**
	 * 关闭 WebSocket 连接
	 */
	void close() throws Exception;
}
