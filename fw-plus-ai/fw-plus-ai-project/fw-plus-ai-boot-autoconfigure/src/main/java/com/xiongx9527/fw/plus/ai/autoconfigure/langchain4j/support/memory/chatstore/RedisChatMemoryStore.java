package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * 基于 Redis 的 {@link dev.langchain4j.store.memory.chat.ChatMemoryStore}（逐条消息存储）。
 *
 * <p>结构：Hash，键为 {@code keyPrefix + tenantId + ":" + memoryId}，field 为消息序号（0 起），
 * value 为单条消息的「单元素 JSON 数组」（消息类型信息在 JSON 内部）；支持可选 TTL，每次写入刷新。</p>
 *
 * <p>{@link StringRedisTemplate} 通过 {@link Supplier} 每次调用时动态解析（类似
 * {@code MagicDynamicEmbeddingStore}），magic-api redis 数据源运行期变更即时生效。</p>
 */
public class RedisChatMemoryStore extends AbstractJsonChatMemoryStore {

    /** 默认键前缀 */
    public static final String DEFAULT_KEY_PREFIX = "fw:ai:chat-memory:";

    private final Supplier<StringRedisTemplate> redisTemplateSupplier;

    private final String keyPrefix;

    /** 过期时间；{@code null} 表示永不过期 */
    private final Duration ttl;

    public RedisChatMemoryStore(Supplier<StringRedisTemplate> redisTemplateSupplier,
            ChatMemoryTenantResolver tenantResolver, String defaultBizType) {
        this(redisTemplateSupplier, tenantResolver, defaultBizType, DEFAULT_KEY_PREFIX, null);
    }

    public RedisChatMemoryStore(Supplier<StringRedisTemplate> redisTemplateSupplier,
            ChatMemoryTenantResolver tenantResolver, String defaultBizType, String keyPrefix, Duration ttl) {
        super(tenantResolver, defaultBizType);
        if (redisTemplateSupplier == null) {
            throw new IllegalArgumentException("RedisChatMemoryStore: redisTemplateSupplier 不能为空");
        }
        this.redisTemplateSupplier = redisTemplateSupplier;
        this.keyPrefix = keyPrefix == null || keyPrefix.isEmpty() ? DEFAULT_KEY_PREFIX : keyPrefix;
        this.ttl = ttl;
    }

    @Override
    protected List<StoredMessage> readMessages(String tenantId, String memoryId, String bizType) {
        HashOperations<String, String, String> hashOperations = redisTemplate().opsForHash();
        Map<String, String> entries = hashOperations.entries(key(tenantId, memoryId));
        if (entries.isEmpty()) {
            return new ArrayList<>(0);
        }
        // field 为消息序号，按数值排序保证消息顺序
        Map<Integer, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            ordered.put(Integer.valueOf(entry.getKey()), entry.getValue());
        }
        List<StoredMessage> result = new ArrayList<>(ordered.size());
        for (String json : ordered.values()) {
            result.add(new StoredMessage(null, json));
        }
        return result;
    }

    @Override
    protected void writeMessages(String tenantId, String memoryId, String bizType, List<StoredMessage> messages) {
        StringRedisTemplate redisTemplate = redisTemplate();
        String key = key(tenantId, memoryId);
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        // 原位覆盖（hset），仅裁剪超长部分，不整键先删后写
        Long existing = hashOperations.size(key);
        int existingCount = existing == null ? 0 : existing.intValue();
        for (int i = 0; i < messages.size(); i++) {
            hashOperations.put(key, String.valueOf(i), messages.get(i).getJson());
        }
        if (existingCount > messages.size()) {
            Object[] surplusKeys = new Object[existingCount - messages.size()];
            for (int i = messages.size(); i < existingCount; i++) {
                surplusKeys[i - messages.size()] = String.valueOf(i);
            }
            hashOperations.delete(key, surplusKeys);
        }
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.expire(key, ttl);
        }
    }

    @Override
    protected void removeMessages(String tenantId, String memoryId) {
        redisTemplate().delete(key(tenantId, memoryId));
    }

    private StringRedisTemplate redisTemplate() {
        StringRedisTemplate redisTemplate = redisTemplateSupplier.get();
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisChatMemoryStore: 无法解析 StringRedisTemplate 客户端");
        }
        return redisTemplate;
    }

    private String key(String tenantId, String memoryId) {
        return keyPrefix + tenantId + ChatMemoryIds.SEPARATOR + memoryId;
    }
}
