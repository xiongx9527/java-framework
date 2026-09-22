package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;
import java.util.Map;

/**
 * MCP 提示词信息
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPromptInfo {
	private String name;
	private String description;
	private List<McpPromptArgument> arguments;

	public McpPromptInfo() {
	}

	public McpPromptInfo(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String description() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<McpPromptArgument> arguments() {
		return arguments;
	}

	public void setArguments(List<McpPromptArgument> arguments) {
		this.arguments = arguments;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("name", name);
		if (description != null) map.put("description", description);
		if (arguments != null) {
			List<Map<String, Object>> args = new java.util.ArrayList<>();
			for (McpPromptArgument arg : arguments) {
				args.add(arg.toMap());
			}
			map.put("arguments", args);
		}
		return map;
	}
}
