package com.xiongx9527.fw.plus.auth.social;

/**
 * 社交登录 token 缓存接口。
 *
 * <p>框架只定义契约，具体存储（本地内存 / Redis / 分布式缓存等）由业务侧实现，
 * 并通过 {@link AbstractSocialLoginProvider#setTokenStore(SocialTokenStore)} 注入。
 * 主要缓存两类 token：</p>
 * <ul>
 *     <li>应用级 token：企业微信 gettoken、飞书 app_access_token、钉钉 accessToken 等，
 *     同一 IdP 配置下所有用户共享、带 TTL，用于避免频繁换取与触发厂商频率限制；</li>
 *     <li>用户级 token：微信扫码 access_token（含 refresh_token），因授权 code 一次性，
 *     失效后只能依赖 refresh_token 续期。</li>
 * </ul>
 */
public interface SocialTokenStore {

    /**
     * 取应用级 token，未命中或已过期返回 {@code null}。
     *
     * @param key 业务侧约定键，通常形如 {@code wecom:app:<configId>:<corpid>} 或 {@code wechat-scan:user:<configId>}
     */
    String getAppToken(String key);

    /**
     * 写入应用级 token。
     *
     * @param key         键
     * @param token       access_token
     * @param ttlSeconds 有效期（秒）
     */
    void putAppToken(String key, String token, long ttlSeconds);

    /**
     * 取用户级 token，未命中或已过期返回 {@code null}。
     *
     * @param key 业务侧约定键，通常形如 {@code wechat-scan:user:<id>:<unionid>}
     */
    CachedUserToken getUserToken(String key);

    /**
     * 写入用户级 token。
     */
    void putUserToken(String key, CachedUserToken token);

    /**
     * 失效应用级 token 缓存（检测到失效时调用，触发下次从远程重取）。
     */
    void evictAppToken(String key);

    /**
     * 失效用户级 token 缓存。
     */
    void evictUserToken(String key);
}
