package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * 提示词请求
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPromptRequest {
	private String name;
	private Map<String, Object> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}
}
