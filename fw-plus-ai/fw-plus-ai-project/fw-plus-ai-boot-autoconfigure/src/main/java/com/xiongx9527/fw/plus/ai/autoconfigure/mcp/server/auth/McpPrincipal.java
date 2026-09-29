package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import java.util.Collections;
import java.util.Set;

/**
 * 经过认证的 MCP 主体。
 * <p>
 * 本次不落地认证授权，仅保留占位；后续认证 change 将真实主体与 scope 绑定到会话。
 * </p>
 */
public class McpPrincipal {

    /**
     * 未认证主体：认证 SPI 的默认放行结果，恒等效于旧单实例行为。
     */
    public static final McpPrincipal ANONYMOUS = new McpPrincipal("anonymous");

    /** 认证关闭时的放行主体。 */
    public static final McpPrincipal PERMIT_ALL = new McpPrincipal("permit-all");

    private final String subject;

    private final Set<String> scopes;

    public McpPrincipal(String subject) {
        this(subject, Collections.emptySet());
    }

    public McpPrincipal(String subject, Set<String> scopes) {
        this.subject = subject;
        this.scopes = scopes == null ? Collections.emptySet() : Collections.unmodifiableSet(scopes);
    }

    public String getSubject() {
        return subject;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}