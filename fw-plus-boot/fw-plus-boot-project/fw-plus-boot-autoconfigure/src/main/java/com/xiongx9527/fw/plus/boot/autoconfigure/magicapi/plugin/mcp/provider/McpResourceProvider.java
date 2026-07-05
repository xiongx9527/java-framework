package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

public interface McpResourceProvider {

	McpPage<McpResourceDefinition> listResources(McpProviderContext context, McpPageRequest pageRequest);

	McpResourceReadResult readResource(McpProviderContext context, McpResourceReadRequest request);

	McpPage<McpResourceTemplate> listTemplates(McpProviderContext context, McpPageRequest pageRequest);
}
