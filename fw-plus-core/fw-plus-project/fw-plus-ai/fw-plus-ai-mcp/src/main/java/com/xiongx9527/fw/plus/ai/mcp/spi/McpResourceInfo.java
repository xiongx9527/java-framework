package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * MCP 资源信息
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpResourceInfo {
	private String uri;
	private String name;
	private String description;
	private String mimeType;

	public McpResourceInfo() {
	}

	public McpResourceInfo(String uri, String name) {
		this.uri = uri;
		this.name = name;
	}

	public String uri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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
		map.put("uri", uri);
		map.put("name", name);
		if (description != null) map.put("description", description);
		if (mimeType != null) map.put("mimeType", mimeType);
		return map;
	}
}
