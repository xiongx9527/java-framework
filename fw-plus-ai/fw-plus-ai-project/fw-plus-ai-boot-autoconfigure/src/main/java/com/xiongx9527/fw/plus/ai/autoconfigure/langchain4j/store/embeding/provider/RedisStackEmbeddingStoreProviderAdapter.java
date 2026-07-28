package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;

public class RedisStackEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "redisstack")
                || ProviderAdapterSupport.supports(provider, "redis");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "host");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "port");
            ProviderAdapterSupport.requiredStringProperty(definition, "indexName");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            return RedisEmbeddingStore.builder()
                    .host(ProviderAdapterSupport.stringProperty(definition, "host"))
                    .port(ProviderAdapterSupport.integerProperty(definition, "port"))
                    .user(ProviderAdapterSupport.stringProperty(definition, "username"))
                    .password(ProviderAdapterSupport.stringProperty(definition, "password"))
                    .indexName(ProviderAdapterSupport.stringProperty(definition, "indexName"))
                    .dimension(ProviderAdapterSupport.integerProperty(definition, "dimension"))
                    .metadataKeys(ProviderAdapterSupport.metadataKeys(definition))
                    .build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
