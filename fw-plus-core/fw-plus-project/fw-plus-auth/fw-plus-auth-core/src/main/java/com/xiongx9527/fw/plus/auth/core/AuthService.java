package com.xiongx9527.fw.plus.auth.core;

/**
 * 实例级认证 SPI：由 {@link InstanceAuthRegistry} 按实例 provider 解析的内置实现提供，
 * 入口侧只做校验，不负责登录、令牌签发或凭据管理。
 * <p>
 * 没有匹配实现（未配置 provider）时请求默认拒绝。
 * </p>
 */
public interface AuthService {

    /**
     * 对一次请求做认证。
     *
     * @param request 请求上下文（栈无关描述）
     * @return 主体；未认证时返回 {@link AuthPrincipal#ANONYMOUS}，不得返回 {@code null}
     */
    AuthPrincipal authenticate(AuthRequest request);
}