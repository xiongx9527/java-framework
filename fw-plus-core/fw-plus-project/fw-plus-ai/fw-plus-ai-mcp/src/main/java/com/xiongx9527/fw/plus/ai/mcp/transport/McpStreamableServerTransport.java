package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

/**
 * MCP Streamable HTTP 服务器传输
 *
 * <p>代表单个 SSE 连接/HTTP 会话。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpStreamableServerTransport extends McpServerTransport {

	/**
	 * 发送消息到客户端（带消息 ID 用于 SSE 事件）
	 *
	 * @param message JSON-RPC 消息
	 * @param messageId 消息 ID（用于 SSE event id）
	 */
	void sendMessage(McpJsonRpcResponse message, String messageId) throws Exception;

	/**
	 * 获取会话 ID
	 */
	String getSessionId();

	/**
	 * 设置会话 ID
	 */
	void setSessionId(String sessionId);
}
