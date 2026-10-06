package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import java.util.Collections;
import java.util.Set;

/**
 * MCP 主体：在 {@link AuthPrincipal} 基础上保留 MCP 专用常量，便于旧代码兼容。
 * <p>
 * 直接继承核心主体，复用 {@code subject}、{@code scopes} 与 {@code isAuthenticated()}。
 * </p>
 */
public class McpPrincipal extends AuthPrincipal {

    /** 兼容旧代码：未认证主体。 */
    public static final McpPrincipal ANONYMOUS = new McpPrincipal("anonymous");

    /** 兼容旧代码：显式放行主体。 */
    public static final McpPrincipal PERMIT_ALL = new McpPrincipal("permit-all");

    public McpPrincipal(String subject) {
        super(subject);
    }

    public McpPrincipal(String subject, Set<String> scopes) {
        super(subject, scopes);
    }

    /**
     * 兼容旧代码：构造匿名主体。
     */
    public static McpPrincipal anonymous() {
        return ANONYMOUS;
    }

    /**
     * 兼容旧代码：构造放行主体。
     */
    public static McpPrincipal permitAll() {
        return PERMIT_ALL;
    }
}