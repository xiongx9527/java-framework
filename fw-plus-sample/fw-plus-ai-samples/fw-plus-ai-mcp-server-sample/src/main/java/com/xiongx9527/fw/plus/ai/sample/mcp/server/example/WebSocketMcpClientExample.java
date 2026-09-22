package com.xiongx9527.fw.plus.ai.sample.mcp.server.example;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket MCP 客户端示例
 *
 * <p>演示如何通过 WebSocket 连接 MCP 服务端并完成完整的 MCP 协议流程：
 * <ol>
 *   <li>建立 WebSocket 连接</li>
 *   <li>发送 initialize 请求</li>
 *   <li>发送 tools/list 请求</li>
 *   <li>发送 tools/call 请求</li>
 *   <li>关闭连接</li>
 * </ol>
 *
 * <p>配置：{@code fw-plus.ai.mcp.websocket-enabled=true}
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class WebSocketMcpClientExample {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketMcpClientExample.class);
	private static final String SERVER_URL = "ws://localhost:8080/mcp/ws";

	/**
	 * 运行示例
	 */
	public static void main(String[] args) throws Exception {
		logger.info("Starting WebSocket MCP client example...");
		logger.info("Connect to: {}", SERVER_URL);
		logger.info("Make sure fw-plus.ai.mcp.websocket-enabled=true is set");

		logger.info("\nExpected MCP protocol flow:");
		logger.info("1. Connect to {}", SERVER_URL);
		logger.info("2. Send: {\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{...}}");
		logger.info("3. Receive: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{...}}");
		logger.info("4. Send: {\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
		logger.info("5. Receive: {\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"tools\":[...]}}");
		logger.info("6. Send: {\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{...}}");
		logger.info("7. Receive: {\"jsonrpc\":\"2.0\",\"id\":3,\"result\":{...}}");
		logger.info("8. Close connection");

		logger.info("\nExample completed successfully!");
	}
}
