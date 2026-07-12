package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class McpPromptResult {

	private String description;

	private List<Map<String, Object>> messages;
}
