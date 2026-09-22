package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

import java.util.Map;

/**
 * MCP Streamable HTTP 会话
 *
 * <p>表示一个客户端连接的会话。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpStreamableServerSession {

	/**
	 * 会话工厂接口
	 */
	interface Factory {
		/**
		 * 创建新会话
		 *
		 * @param sessionId 会话 ID
		 * @return 会话实例
		 */
		McpStreamableServerSession createSession(String sessionId);
	}

	/**
	 * 处理 JSON-RPC 请求
	 */
	McpJsonRpcResponse handleRequest(String sessionId, com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest request);

	/**
	 * 发送通知到客户端
	 */
	void sendNotification(String method, Object params) throws Exception;

	/**
	 * 关闭会话
	 */
	void close() throws Exception;
}
