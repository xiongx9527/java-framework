package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.scanner.fixture;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.xiongx9527.fw.plus.ai.mcp.server.annotation.McpServer;

@McpService
public class WeatherDefaultService {

	@Tool("getWeather")
	public String getWeather() {
		return "sunny";
	}

	@Tool("getWeatherDetail")
	@McpServer
	public String getWeatherDetail() {
		return "sunny and 25c";
	}
}