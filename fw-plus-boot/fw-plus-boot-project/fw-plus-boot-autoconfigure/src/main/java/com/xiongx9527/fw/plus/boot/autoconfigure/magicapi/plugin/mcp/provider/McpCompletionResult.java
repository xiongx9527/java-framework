package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

import java.util.List;

@Data
public class McpCompletionResult {

	private List<String> values;

	private boolean hasMore;
}
