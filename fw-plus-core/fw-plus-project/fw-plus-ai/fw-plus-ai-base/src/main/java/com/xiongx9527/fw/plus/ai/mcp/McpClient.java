package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.List;

/** MCP 客户端端口，镜像 langchain4j {@code langchain4j-mcp} 的 McpClient（Spring AI 等价 McpSyncClient / McpAsyncClient）。 */
public interface McpClient {
    List<ToolSpecification> listTools();
    String callTool(String name, String arguments);
    void close();
}
