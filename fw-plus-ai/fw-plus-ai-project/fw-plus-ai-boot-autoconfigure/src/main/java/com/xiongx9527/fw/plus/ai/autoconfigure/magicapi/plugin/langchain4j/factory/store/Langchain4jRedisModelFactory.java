package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;

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
        return RedisEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .user(o.string("user", null))
                .password(o.string("password", null))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .dimension(dimension)
                .metadataKeys(o.stringList("metadata-keys", null))
                .build();
    }
}
