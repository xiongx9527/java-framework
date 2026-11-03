package com.xiongx9527.fw.plus.ai.base.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存缓存：单机 / 测试用，带纪元失效与 TTL。
 *
 * <p>读写时顺带清理过期条目，避免无界增长；多实例部署请换共享实现。</p>
 */
public class InMemoryCachePort implements CachePort {

    private final Map<String, Entry> entries = new ConcurrentHashMap<String, Entry>();

    private final AtomicLong epoch = new AtomicLong(0L);

    @Override
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt > 0 && System.currentTimeMillis() > entry.expireAt) {
            entries.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void put(String key, Object value, long ttlMillis) {
        if (key == null || value == null) {
            return;
        }
        long expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0L;
        entries.put(key, new Entry(value, expireAt));
    }

    @Override
    public void evict(String key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    @Override
    public long epoch() {
        return epoch.get();
    }

    @Override
    public long bumpEpoch() {
        long next = epoch.incrementAndGet();
        entries.clear();
        return next;
    }

    private static final class Entry {

        private final Object value;

        private final long expireAt;

        private Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
