package com.xiongx9527.fw.plus.ai.base.cache;

/**
 * 缓存端口：读写 + 纪元失效。
 *
 * <p>{@link #epoch()} 是全局版本号，写入时可把它拼进 key；{@link #bumpEpoch()} 后旧键全部不可达，
 * 由 TTL 自然回收。</p>
 */
public interface CachePort {

    Object get(String key);

    void put(String key, Object value, long ttlMillis);

    void evict(String key);

    /** 当前纪元。 */
    long epoch();

    /** 递增纪元（数据变更后调用），返回新值。 */
    long bumpEpoch();
}
