package com.xiongx9527.fw.plus.auth.core;

/**
 * 显式绑定时恒放行的认证实现，适用于本地调试或受信任的专用实例。
 * <p>
 * 返回 {@link AuthPrincipal#PERMIT_ALL} 而非 {@link AuthPrincipal#ANONYMOUS}：
 * 入口只拒绝匿名主体，"关闭认证"因此能与"开了认证但凭据无效"区分开。
 * </p>
 */
public class PermitAllAuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        return AuthPrincipal.PERMIT_ALL;
    }
}