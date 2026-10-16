package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;

/**
 * MCP 请求认证 SPI：适配层，将 {@link AuthService} 统一入口转为 MCP 专用类型。
 * <p>
 * 实际认证逻辑由 {@link InstanceAuthRegistry} 按 provider 解析的内置 {@link AuthService} 实现提供，
 * 入口侧只做校验，不负责登录、令牌签发或凭据管理。
 * <p>
 * 没有匹配实现（未配置 provider）时请求默认拒绝。
 * </p>
 */
@FunctionalInterface
public interface McpAuthService {

    /**
     * 对一次 MCP 请求做认证。
     *
     * @param request 请求上下文
     * @return 主体；未认证时返回 {@link McpPrincipal#ANONYMOUS}，不得返回 {@code null}
     */
    McpPrincipal authenticate(McpRequest request);

    /**
     * 适配器：把核心 {@link AuthService} 包装为 {@link McpAuthService}。
     */
    static McpAuthService adapt(AuthService core) {
        return req -> toMcpPrincipal(core.authenticate(toCore(req)));
    }

    /**
     * 适配器：把旧 SPI {@link McpAuthService} 包装为核心 {@link AuthService}。
     */
    static AuthService adaptToCore(McpAuthService spi) {
        return spi != null ? req -> toCorePrincipal(spi.authenticate(toSpi(req))) : null;
    }

    static AuthRequest toCore(McpRequest req) {
        return new AuthRequest(req.getRuntimeKey(), req.getSessionId(), req.getTransport(),
                req.getEndpoint(), req.getBody(), req.getHeaders());
    }

    static com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest toSpi(
            com.xiongx9527.fw.plus.auth.core.AuthRequest req) {
        return new McpRequest(req.getRuntimeKey(), req.getSessionId(), req.getTransport(),
                req.getEndpoint(), req.getBody(), req.getHeaders());
    }

    static McpPrincipal toMcpPrincipal(AuthPrincipal core) {
        if (core == null) {
            // SPI 契约要求实现不得返回 null，此处兜底为匿名，保证入口 fail-closed 而非 NPE。
            return McpPrincipal.ANONYMOUS;
        }
        if (core == AuthPrincipal.ANONYMOUS) {
            return McpPrincipal.ANONYMOUS;
        }
        if (core == AuthPrincipal.PERMIT_ALL) {
            return McpPrincipal.PERMIT_ALL;
        }
        return new McpPrincipal(core.getSubject(), core.getScopes());
    }

    static AuthPrincipal toCorePrincipal(McpPrincipal spi) {
        if (spi == null) {
            return AuthPrincipal.ANONYMOUS;
        }
        if (spi == McpPrincipal.ANONYMOUS) {
            return AuthPrincipal.ANONYMOUS;
        }
        if (spi == McpPrincipal.PERMIT_ALL) {
            return AuthPrincipal.PERMIT_ALL;
        }
        return new AuthPrincipal(spi.getSubject(), spi.getScopes());
    }
}