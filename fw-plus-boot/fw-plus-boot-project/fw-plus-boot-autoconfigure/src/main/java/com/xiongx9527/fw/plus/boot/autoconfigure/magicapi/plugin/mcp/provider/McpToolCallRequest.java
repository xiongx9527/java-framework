package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
public class McpToolCallRequest {

	private String name;

	private Map<String, Object> arguments = Collections.emptyMap();
}
