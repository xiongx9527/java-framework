package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.xiongx9527.fw.plus.common.debounce.Debounce;
import com.xiongx9527.fw.plus.common.debounce.DebounceBackendResolver;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 接口防抖器：同一「调用方 + 接口」在防抖窗口内只放行一次。
 *
 * <p>两种后端，语义一致但原子性实现不同：</p>
 * <ul>
 *   <li><b>Redis（多实例）</b>：委托 {@link DebounceSlot}（框架内置 Redis 实现） 用 Lua 脚本一次原子完成占位；</li>
 *   <li><b>内存 / 其它</b>：沿用 Spring Cache 的 {@code putIfAbsent} 原子占位（单实例足够）；
 *       占位值存放「本次窗口过期时间戳」，支持按接口自定义窗口（{@link Debounce#windowMs()}）。</li>
 * </ul>
 *
 * <p>缓存 / Redis 异常时<b>放行</b>（防抖是保护措施，不应因存储故障而中断业务）。</p>
 */
public class RequestDebouncer {

    private static final Logger log = LoggerFactory.getLogger(RequestDebouncer.class);

    private final CacheManager cacheManager;

    private final DebounceProperties properties;

    private final ObjectProvider<DebounceSlot> slotProvider;

    private final ObjectProvider<DebounceBackendResolver> backendResolverProvider;

    /** 兼容单测 / 仅 Cache 后端的简化构造（不使用 Redis 原子脚本）。 */
    public RequestDebouncer(CacheManager cacheManager, DebounceProperties properties) {
        this(cacheManager, properties, null, null);
    }

    public RequestDebouncer(CacheManager cacheManager, DebounceProperties properties,
                            ObjectProvider<DebounceSlot> slotProvider,
                            ObjectProvider<DebounceBackendResolver> backendResolverProvider) {
        this.cacheManager = cacheManager;
        this.properties = properties;
        this.slotProvider = slotProvider;
        this.backendResolverProvider = backendResolverProvider;
    }

    /**
     * 判定是否放行。
     *
     * @param key       调用方 + 接口 维度的键
     * @param windowMs  本次生效的防抖窗口（毫秒，&gt; 0；会被收敛到上限）
     * @return true 放行；false 表示窗口内重复请求，应返回 429
     */
    public boolean allow(String key, long windowMs) {
        if (!properties.isEnabled() || key == null) {
            return true;
        }
        long window = Math.max(1L, Math.min(windowMs, DebounceProperties.MAX_WINDOW_MS));
        if (useRedisBackend()) {
            DebounceSlot slot = slotProvider.getIfAvailable();
            if (slot != null) {
                return slot.acquire(properties.getRedisKeyPrefix() + key, window);
            }
        }
        return allowWithCache(key, window);
    }

    /**
     * 后端选择：Redis 槽可用时默认走 Redis；业务侧提供 {@link DebounceBackendResolver} 时以其为准。
     */
    private boolean useRedisBackend() {
        if (slotProvider == null || slotProvider.getIfAvailable() == null) {
            return false;
        }
        DebounceBackendResolver resolver = backendResolverProvider == null
                ? null : backendResolverProvider.getIfAvailable();
        return resolver == null || resolver.useRedis();
    }

    /** 单实例：Spring Cache 的 putIfAbsent 原子占位 + 窗口过期后重新占位。 */
    private boolean allowWithCache(String key, long window) {
        try {
            Cache cache = cacheManager.getCache(properties.getCacheName());
            if (cache == null) {
                return true;
            }
            long now = System.currentTimeMillis();
            long expiry = now + window;
            // putIfAbsent 原子占位：返回 null 表示此前不存在 → 本次放行
            Cache.ValueWrapper existing = cache.putIfAbsent(key, expiry);
            if (existing == null) {
                return true;
            }
            Long prevExpiry = asLong(existing.get());
            // 占位已过期（窗口已过）但尚未被 TTL 回收 → 重新占位后放行
            if (prevExpiry == null || now > prevExpiry) {
                cache.put(key, expiry);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.info("接口防抖判定失败（放行）: {}", e.getMessage());
            return true;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
