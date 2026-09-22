package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 提示词参数
 *
 * <p>对齐 MCP 规范 PromptArgument：name、description、required。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPromptArgument {
	private String name;
	private String description;
	private boolean required;

	public McpPromptArgument() {
	}

	public McpPromptArgument(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public McpPromptArgument(String name, String description, boolean required) {
		this.name = name;
		this.description = description;
		this.required = required;
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

	public boolean required() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", name);
		if (description != null) {
			map.put("description", description);
		}
		if (required) {
			map.put("required", required);
		}
		return map;
	}
}
