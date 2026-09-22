package com.xiongx9527.fw.plus.ai.mcp.spi;

/**
 * 进度 Token
 *
 * <p>用于工具调用中的进度报告。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpProgressToken {
    private final Object token;

    public McpProgressToken(Object token) {
        this.token = token;
    }

    public Object getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McpProgressToken that = (McpProgressToken) o;
        return token != null && token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }
}
