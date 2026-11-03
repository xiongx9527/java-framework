package com.xiongx9527.fw.plus.ai.base.mcp;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * MCP 调用事件：工具列举 / 工具调用 / 失败。
 */
public interface McpListener extends AiListener {

    /** 工具列表加载完成。 */
    default void onListTools(String clientName, int toolCount) {
    }

    /** 工具调用完成。 */
    default void onToolCall(String clientName, String toolName) {
    }
}
