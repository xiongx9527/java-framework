package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.scanner.fixture;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.xiongx9527.fw.plus.ai.mcp.server.annotation.McpServer;

@McpService
@McpServer("order")
public class OrderService {

	@Tool("createOrder")
	public String createOrder() {
		return "created";
	}

	@Tool("getOrder")
	@McpServer("weather")
	public String getOrder() {
		return "weather";
	}
}