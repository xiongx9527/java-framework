package com.xiongx9527.fw.plus.auth.core;

/**
 * fail-closed 拒绝服务：恒返回 {@link AuthPrincipal#ANONYMOUS}，请求一律被拒。
 * <p>
 * 用于实例已配置某 provider、但对应模块不在 classpath 的场景：
 * 不回退到其它认证方式，也不使启动失败，仅拒绝该实例的全部请求。
 * </p>
 */
public class RejectingAuthService implements AuthService {

    public static final RejectingAuthService INSTANCE = new RejectingAuthService();

    private RejectingAuthService() {
    }

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        return AuthPrincipal.ANONYMOUS;
    }
}