package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

public interface McpPromptProvider {

	McpPage<McpPromptDefinition> listPrompts(McpProviderContext context, McpPageRequest pageRequest);

	McpPromptResult getPrompt(McpProviderContext context, McpPromptGetRequest request);
}
