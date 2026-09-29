package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth;

/**
 * 默认认证实现：恒放行。
 *
 * <p>返回 {@link McpPrincipal#PERMIT_ALL} 而非 {@link McpPrincipal#ANONYMOUS}：
 * controller 只拒绝匿名主体，"关闭认证"因此能与"开了认证但 token 无效"区分开。</p>
 */
public class PermitAllAuthService implements McpAuthService {

    @Override
    public McpPrincipal authenticate(McpRequest request) {
        return McpPrincipal.PERMIT_ALL;
    }
}