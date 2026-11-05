package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;

/**
 * 用户级 token 缓存值。access_token 过期时可用 {@link #getRefreshToken()} 续期
 * （微信扫码授权 code 一次性，无法重置换，必须依赖 refresh_token）。
 */
public class CachedUserToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String accessToken;
    private final String refreshToken;
    private final String openid;
    private final String unionid;
    private final long expiresAt;

    public CachedUserToken(String accessToken, String refreshToken, String openid, String unionid, long expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.openid = openid;
        this.unionid = unionid;
        this.expiresAt = expiresAt;
    }

    /**
     * 是否已过期（已留 skewMillis 余量，提前失效以便续期）。
     */
    public boolean isExpired(long skewMillis) {
        return System.currentTimeMillis() + skewMillis >= expiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getOpenid() {
        return openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
