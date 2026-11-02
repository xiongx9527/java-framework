package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.List;

/** 把 MCP 服务端工具暴露为 base 的 {@link ToolProvider}，供 ChatModel 工具调用环使用。 */
public class McpToolProvider implements ToolProvider {
    private final McpClient client;
    public McpToolProvider(McpClient client) { this.client = client; }
    @Override public List<ToolSpecification> provide() { return client.listTools(); }
    public String execute(String name, String arguments) { return client.callTool(name, arguments); }
}
