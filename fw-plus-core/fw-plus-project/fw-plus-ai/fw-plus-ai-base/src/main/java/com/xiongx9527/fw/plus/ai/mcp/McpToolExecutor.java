package com.xiongx9527.fw.plus.ai.mcp;

/** MCP 工具执行回调：给定 JSON 参数，返回 JSON 结果（由具体后端 adapter 实现）。 */
public interface McpToolExecutor {
    String execute(String arguments);
}
