package com.xiongx9527.fw.plus.auth.social;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 基于 Guava {@link Cache} 的默认内存实现，供开发、测试、单机场景使用。
 *
 * <p>应用级 token 按写入 TTL 惰性过期；用户级 token 过期判断交由调用方（含 refresh_token 续期）。
 * 生产环境建议由业务侧注入 Redis 等分布式实现
 * （{@link AbstractSocialLoginProvider#setTokenStore(SocialTokenStore)}）。</p>
 */
public class LocalSocialTokenStore implements SocialTokenStore {

    private static final class AppEntry {
        final String token;
        final long expireAt;

        AppEntry(String token, long expireAt) {
            this.token = token;
            this.expireAt = expireAt;
        }
    }

    private final Cache<String, AppEntry> appTokens =
            CacheBuilder.newBuilder().maximumSize(10000).build();
    private final Cache<String, CachedUserToken> userTokens =
            CacheBuilder.newBuilder().maximumSize(10000).build();

    @Override
    public String getAppToken(String key) {
        AppEntry entry = appTokens.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() >= entry.expireAt) {
            appTokens.invalidate(key);
            return null;
        }
        return entry.token;
    }

    @Override
    public void putAppToken(String key, String token, long ttlSeconds) {
        long expireAt = System.currentTimeMillis() + Math.max(ttlSeconds, 1L) * 1000L;
        appTokens.put(key, new AppEntry(token, expireAt));
    }

    @Override
    public CachedUserToken getUserToken(String key) {
        CachedUserToken token = userTokens.getIfPresent(key);
        if (token != null && token.isExpired(0L)) {
            userTokens.invalidate(key);
            return null;
        }
        return token;
    }

    @Override
    public void putUserToken(String key, CachedUserToken token) {
        userTokens.put(key, token);
    }

    @Override
    public void evictAppToken(String key) {
        appTokens.invalidate(key);
    }

    @Override
    public void evictUserToken(String key) {
        userTokens.invalidate(key);
    }
}
