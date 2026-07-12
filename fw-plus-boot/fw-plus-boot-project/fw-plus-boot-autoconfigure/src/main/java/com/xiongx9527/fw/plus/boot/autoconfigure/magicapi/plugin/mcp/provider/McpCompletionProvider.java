package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

public interface McpCompletionProvider {

	McpCompletionResult complete(McpProviderContext context, McpCompletionRequest request);
}
