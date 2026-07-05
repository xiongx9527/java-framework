package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Map;

@Data
public class McpCompletionRequest {

	private Map<String, Object> ref;

	private Map<String, Object> argument;
}
