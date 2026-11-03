package com.xiongx9527.fw.plus.ai.base.mcp;

import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端：连上一个 MCP 服务，列举并调用它的工具。
 *
 * <p>实现负责传输与握手（STDIO / SSE / Streamable HTTP），调用方只看到「工具列表 + 调用」。</p>
 */
public interface McpClient extends AutoCloseable {

    /** 连接配置。 */
    McpClientConfig config();

    /** 列举服务端工具；失败时抛 {@link McpException}。 */
    List<ToolSpecification> listTools();

    /** 调用工具；参数为已解析的键值对，返回结果文本。 */
    String callTool(String toolName, Map<String, Object> arguments);

    @Override
    default void close() {
        // 无连接可关的客户端不需要实现
    }
}
