package com.xiongx9527.fw.plus.auth.social;

/**
 * 社交/企业登录授权 Provider SPI。
 */
public interface SocialLoginProvider {

    /**
     * @return 该 Provider 处理的身份提供方类型
     */
    IdpType type();

    /**
     * 构造前端跳转的授权 URL。
     */
    String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request);

    /**
     * 返回前端需要的客户端信息（如企业微信 corpid/agentid）。
     * 非 IM 类可返回 {@code null}。
     */
    default ClientInfo clientInfo(IdpConfig config) {
        return null;
    }

    /**
     * 用回调凭证解析出外部身份（OAuth/扫码类授权）。
     */
    ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback);

    /**
     * 该 Provider 是否支持账密登录（如 LDAP）。
     */
    default boolean supportsPasswordLogin() {
        return false;
    }

    /**
     * 账密登录（仅 {@link #supportsPasswordLogin()} 为 true 的 Provider 实现）。
     */
    default ExternalIdentity loginWithPassword(IdpConfig config, String username, String password) {
        throw new UnsupportedOperationException("password login not supported by " + type());
    }
}
