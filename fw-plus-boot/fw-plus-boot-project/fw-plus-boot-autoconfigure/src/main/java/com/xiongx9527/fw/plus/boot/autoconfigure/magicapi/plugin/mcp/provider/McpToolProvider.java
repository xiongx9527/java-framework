package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

public interface McpToolProvider {

	McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest);

	McpToolResult callTool(McpProviderContext context, McpToolCallRequest request);
}
