package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Map;

@Data
public class McpToolDefinition {

	private String name;

	private String description;

	private Map<String, Object> inputSchema;

	private Map<String, Object> outputSchema;

	private Map<String, Object> annotations;
}
