package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 工具定义
 *
 * <p>字段对齐 MCP 2025-11-25 规范：name、description、title、inputSchema、annotations、icon。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpTool {
	private String name;
	private String description;
	private String title;
	private Map<String, Object> inputSchema;
	private Map<String, Object> annotations;
	private String icon;

	public McpTool() {
	}

	public McpTool(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public static McpToolBuilder builder() {
		return new McpToolBuilder();
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

	public String title() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, Object> inputSchema() {
		return inputSchema;
	}

	public void setInputSchema(Map<String, Object> inputSchema) {
		this.inputSchema = inputSchema;
	}

	public Map<String, Object> annotations() {
		return annotations;
	}

	public void setAnnotations(Map<String, Object> annotations) {
		this.annotations = annotations;
	}

	public String icon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", name);
		if (description != null) {
			map.put("description", description);
		}
		if (title != null) {
			map.put("title", title);
		}
		if (inputSchema != null) {
			map.put("inputSchema", inputSchema);
		}
		if (annotations != null && !annotations.isEmpty()) {
			map.put("annotations", annotations);
		}
		if (icon != null) {
			map.put("icon", icon);
		}
		return map;
	}

	public static class McpToolBuilder {
		private String name;
		private String description;
		private String title;
		private Map<String, Object> inputSchema;
		private Map<String, Object> annotations;
		private String icon;

		public McpToolBuilder name(String name) {
			this.name = name;
			return this;
		}

		public McpToolBuilder description(String description) {
			this.description = description;
			return this;
		}

		public McpToolBuilder title(String title) {
			this.title = title;
			return this;
		}

		public McpToolBuilder inputSchema(Map<String, Object> inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public McpToolBuilder annotations(Map<String, Object> annotations) {
			this.annotations = annotations;
			return this;
		}

		public McpToolBuilder icon(String icon) {
			this.icon = icon;
			return this;
		}

		public McpTool build() {
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Tool name is required");
			}
			McpTool tool = new McpTool();
			tool.setName(name);
			tool.setDescription(description);
			tool.setTitle(title);
			tool.setInputSchema(inputSchema);
			tool.setAnnotations(annotations);
			tool.setIcon(icon);
			return tool;
		}
	}
}
