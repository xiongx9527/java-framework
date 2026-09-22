package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;

/**
 * MCP 服务器传输接口
 *
 * <p>表示与客户端的单个连接/会话。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpServerTransport {

	/**
	 * 发送 JSON-RPC 请求/响应
	 */
	void send(McpJsonRpcResponse response) throws Exception;

	/**
	 * 接收 JSON-RPC 请求
	 */
	McpJsonRpcRequest receive() throws Exception;

	/**
	 * 关闭传输
	 */
	void close() throws Exception;
}
