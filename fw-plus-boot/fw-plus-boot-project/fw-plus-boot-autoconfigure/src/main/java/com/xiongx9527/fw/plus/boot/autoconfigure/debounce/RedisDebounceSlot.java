package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import org.slf4j.Logger;
import com.xiongx9527.fw.plus.common.debounce.DebounceSlot;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * Redis 原子占位：用一段 Lua 脚本「读占位 + 比较过期时间 + PSETEX 占位」一次原子完成。
 *
 * <p>不能只用缓存的 {@code putIfAbsent}：占位是通过值里的过期时间复用的，窗口刚过期时两个实例
 * 会同时读到旧值并各自重新占位，导致并发重复提交都被放行。Lua 在 Redis 内串行执行，
 * 跨实例严格只放行一次，且占位 TTL 恰为本次窗口，过期即自然消失。</p>
 *
 * <p>单独成类是为了让 {@link RequestDebouncer} 不直接依赖 spring-data-redis：
 * 未引入 Redis 时只装配 Cache 后端即可。Redis 异常时<b>放行</b>（防抖不应中断业务）。</p>
 */
public class RedisDebounceSlot implements DebounceSlot {

    private static final Logger log = LoggerFactory.getLogger(RedisDebounceSlot.class);

    /**
     * 原子占位脚本：占位不存在或已过期（值 ≤ 当前时间）则写入新过期时间并返回 1，否则返回 0。
     *
     * <p>ARGV[1]=新过期时间戳，ARGV[2]=当前时间戳，ARGV[3]=窗口毫秒数（同时作为 TTL）。</p>
     */
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "local cur = redis.call('GET', KEYS[1]) "
                    + "if (not cur) or tonumber(cur) <= tonumber(ARGV[2]) then "
                    + "  redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[1]) "
                    + "  return 1 "
                    + "end "
                    + "return 0",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDebounceSlot(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param key      Redis 占位键（调用方 + 接口维度）
     * @param windowMs 本次生效窗口（毫秒，&gt; 0）
     * @return true 放行；false 表示窗口内重复请求
     */
    @Override
    public boolean acquire(String key, long windowMs) {
        long now = System.currentTimeMillis();
        long expiry = now + windowMs;
        try {
            Long acquired = redisTemplate.execute(ACQUIRE_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(expiry), String.valueOf(now), String.valueOf(windowMs));
            return acquired != null && acquired == 1L;
        } catch (Exception e) {
            log.info("接口防抖判定失败（放行）: {}", e.getMessage());
            return true;
        }
    }
}
