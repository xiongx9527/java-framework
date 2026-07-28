package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Map;

@Data
public class McpResourceTemplate {

	private String uriTemplate;

	private String name;

	private String description;

	private String mimeType;

	private Map<String, Object> annotations;
}
