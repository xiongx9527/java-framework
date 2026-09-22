package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * MCP 资源模板
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpResourceTemplate {
	private String uriTemplate;
	private String name;
	private String description;
	private String mimeType;

	public McpResourceTemplate() {
	}

	public McpResourceTemplate(String uriTemplate, String name) {
		this.uriTemplate = uriTemplate;
		this.name = name;
	}

	public String uriTemplate() {
		return uriTemplate;
	}

	public void setUriTemplate(String uriTemplate) {
		this.uriTemplate = uriTemplate;
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

	public String mimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("uriTemplate", uriTemplate);
		map.put("name", name);
		if (description != null) map.put("description", description);
		if (mimeType != null) map.put("mimeType", mimeType);
		return map;
	}
}
