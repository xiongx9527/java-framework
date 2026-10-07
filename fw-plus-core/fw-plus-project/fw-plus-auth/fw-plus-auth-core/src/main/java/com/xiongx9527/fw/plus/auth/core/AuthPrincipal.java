package com.xiongx9527.fw.plus.auth.core;

import java.util.Collections;
import java.util.Set;

/**
 * 经过认证的主体。
 * <p>
 * 包含标识符与可选的 scopes 集合。scopes 可为多值或空集，用于 capability 裁剪。
 * </p>
 */
public class AuthPrincipal {

    /**
     * 未认证主体：入口必须拒绝，不得作为放行结果。
     */
    public static final AuthPrincipal ANONYMOUS = new AuthPrincipal("anonymous");

    /** 仅显式配置放行认证服务时使用的主体（绕过 scope 裁剪）。 */
    public static final AuthPrincipal PERMIT_ALL = new AuthPrincipal("permit-all");

    private final String subject;

    private final Set<String> scopes;

    public AuthPrincipal(String subject) {
        this(subject, Collections.emptySet());
    }

    public AuthPrincipal(String subject, Set<String> scopes) {
        this.subject = subject;
        this.scopes = scopes == null ? Collections.emptySet() : Collections.unmodifiableSet(scopes);
    }

    public String getSubject() {
        return subject;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public boolean isAuthenticated() {
        return subject != null && !subject.trim().isEmpty() && !"anonymous".equals(subject);
    }
}