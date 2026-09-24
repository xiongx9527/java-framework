package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth;

/**
 * 默认认证实现：恒放行，返回匿名主体，等效于旧的单实例无认证行为。
 */
public class PermitAllAuthService implements McpAuthService {

    @Override
    public McpPrincipal authenticate(McpRequest request) {
        return McpPrincipal.ANONYMOUS;
    }
}