package com.xiongx9527.fw.plus.common.debounce;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口防抖配置（{@code fw.plus.debounce.*}）。
 *
 * <ul>
 *   <li>{@code enabled}：是否启用（缺省关闭，避免改变既有行为）；</li>
 *   <li>{@code window-ms}：防抖窗口（毫秒，缺省 1000）——同一维度在该窗口内只放行一次，
 *       重复请求直接返回 429；同时作为底层缓存 TTL；</li>
 *   <li>{@code cache-name}：进程内/Spring Cache 后端的缓存区域名；</li>
 *   <li>{@code redis-key-prefix}：Redis 原子占位键前缀。</li>
 * </ul>
 *
 * <p>防抖的生效范围不再由路径配置，而是由控制器方法上的 {@link Debounce} 注解决定。</p>
 */
@ConfigurationProperties(prefix = DebounceProperties.PREFIX)
public class DebounceProperties {

    /** 配置前缀。 */
    public static final String PREFIX = "fw.plus.debounce";

    /**
     * 单接口防抖窗口上限（毫秒）。注解 {@code @Debounce(windowMs=...)} 的取值会被收敛到此上限，
     * 以保证缓存占位（底层 TTL）足以覆盖逻辑窗口，避免提前过期导致误放行。
     */
    public static final long MAX_WINDOW_MS = 600_000L;

    /** 是否启用接口防抖。 */
    private boolean enabled = false;

    /** 防抖窗口（毫秒）。 */
    private long windowMs = 1000L;

    /** Spring Cache 后端的缓存区域名（Redis 后端会再叠加应用自己的 key 前缀）。 */
    private String cacheName = "fw.plus:debounce";

    /** Redis 原子占位键前缀（与 Spring Cache 的区域名区分，避免值格式冲突）。 */
    private String redisKeyPrefix = "fw.plus:debounce:slot:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getWindowMs() {
        return windowMs;
    }

    public void setWindowMs(long windowMs) {
        this.windowMs = windowMs;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
