package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

/**
 * MCP 认证 SPI 占位。
 * <p>
 * 本次仅保留接口面，默认放行；认证（Sa-Token 应用内会话 / OAuth2+JWKS 外部 AS）与
 * scope 裁剪为后续独立 change，届时只替换实现，不修改 handler 签名。
 * </p>
 */
public interface McpAuthService {

    /**
     * 对一次 MCP 请求做认证。
     *
     * @param request 请求上下文
     * @return 主体；未认证时为 {@link McpPrincipal#ANONYMOUS}
     */
    McpPrincipal authenticate(McpRequest request);
}