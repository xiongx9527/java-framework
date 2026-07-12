package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class McpToolResult {

	private List<Map<String, Object>> content = Collections.emptyList();

	private Object structuredContent;

	private boolean error;
}
