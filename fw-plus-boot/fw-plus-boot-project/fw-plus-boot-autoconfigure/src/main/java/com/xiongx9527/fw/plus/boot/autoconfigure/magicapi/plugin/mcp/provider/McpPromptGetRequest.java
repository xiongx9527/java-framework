package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Map;

@Data
public class McpPromptGetRequest {

	private String name;

	private Map<String, Object> arguments;
}
