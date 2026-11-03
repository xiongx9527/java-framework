package com.xiongx9527.fw.plus.ai.base.mcp;

import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.List;
import java.util.Map;

/**
 * MCP 服务端：把本系统的工具以 MCP 暴露给外部调用方。
 *
 * <p>只描述「暴露什么、怎么执行」，进程启动与传输绑定由
 * {@code fw-plus-ai-mcp-server} 落地。</p>
 */
public interface McpServer {

    /** 服务名。 */
    String name();

    /** 暴露的工具规格。 */
    List<ToolSpecification> specifications();

    /** 执行工具调用。 */
    String invoke(String toolName, Map<String, Object> arguments);
}
