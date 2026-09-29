package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

/**
 * Redis 向量库工厂：写法参照 core 层 fw-plus-ai-langchain4j-redis 的 AutoConfig。
 * RedisEmbeddingStore.builder() 静态方法返回 Builder。
 */
public class Langchain4jRedisModelFactory implements ModelFactory {

    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    @Override
    public String provider() {
        return "redis";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        // 1.20.0-beta30 的 RedisEmbeddingStore 改为注入 UnifiedJedis（不再提供 host/port builder）
        String host = o.string("host", DEFAULT_HOST);
        int port = o.integer("port", DEFAULT_PORT);
        String user = o.string("user", null);
        String password = o.string("password", null);
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder();
        if (user != null) {
            config.user(user);
        }
        if (password != null) {
            config.password(password);
        }
        UnifiedJedis jedis = new JedisPooled(new HostAndPort(host, port), config.build());
        return RedisEmbeddingStore.builder()
                .unifiedJedis(jedis)
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .dimension(dimension)
                .metadataKeys(o.stringList("metadata-keys", null))
                .build();
    }
}
