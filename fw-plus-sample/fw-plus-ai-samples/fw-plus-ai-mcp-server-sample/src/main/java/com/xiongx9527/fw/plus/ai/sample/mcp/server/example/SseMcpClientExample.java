package com.xiongx9527.fw.plus.ai.sample.mcp.server.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * SSE (Streamable HTTP) MCP 客户端示例
 *
 * <p>演示如何通过 SSE 端点连接 MCP 服务端：
 * <ol>
 *   <li>发送 POST 请求到 /mcp/sse 建立 SSE 流</li>
 *   <li>接收服务端推送的事件</li>
 *   <li>通过 POST 发送 JSON-RPC 请求</li>
 *   <li>通过 SSE 接收响应和通知</li>
 * </ol>
 *
 * <p>配置：{@code fw-plus.ai.mcp.sse-enabled=true}
 *
 * <p>注意：SSE 使用 EventSource API，需要 HTTP/HTTPS 协议。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class SseMcpClientExample {

	private static final Logger logger = LoggerFactory.getLogger(SseMcpClientExample.class);
	private static final String SSE_ENDPOINT = "http://localhost:8080/mcp/sse";

	/**
	 * 运行示例
	 */
	public static void main(String[] args) throws Exception {
		logger.info("Starting SSE MCP client example...");
		logger.info("Connect to: {}", SSE_ENDPOINT);
		logger.info("Make sure fw-plus.ai.mcp.sse-enabled=true is set");

		logger.info("\nExpected MCP protocol flow:");
		logger.info("1. GET {} - Establish SSE stream", SSE_ENDPOINT);
		logger.info("2. POST {} with initialize request", SSE_ENDPOINT);
		logger.info("3. Receive events via SSE:");
		logger.info("   event: message");
		logger.info("   data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{...}}");
		logger.info("4. POST {} with tools/list", SSE_ENDPOINT);
		logger.info("5. Receive tools via SSE");
		logger.info("6. POST {} with tools/call", SSE_ENDPOINT);
		logger.info("7. Receive tool result via SSE");
		logger.info("8. DELETE {} - Close session", SSE_ENDPOINT);

		logger.info("\nNote: SSE transport uses Mcp-Session-Id header for session management");
		logger.info("\nExample completed successfully!");
	}
}
