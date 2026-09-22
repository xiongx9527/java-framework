package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * 工具调用请求
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpToolCallRequest {
    private String name;
    private Map<String, Object> arguments;
    private McpProgressToken progressToken;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public McpProgressToken getProgressToken() {
        return progressToken;
    }

    public void setProgressToken(McpProgressToken progressToken) {
        this.progressToken = progressToken;
    }
}
