package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;

/** MCP 服务端端口，把 base 工具以 MCP 协议暴露给外部（stdio / http-sse），镜像 langchain4j 的 McpServer（Spring AI 等价 McpServer 传输）。 */
public interface McpServer {
    void registerTool(ToolSpecification spec, McpToolExecutor executor);
    void start();
    void stop();
}
