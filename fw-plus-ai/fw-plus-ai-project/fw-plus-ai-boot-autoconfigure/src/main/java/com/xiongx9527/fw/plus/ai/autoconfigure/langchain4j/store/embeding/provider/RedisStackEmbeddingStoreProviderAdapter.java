package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;

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
            String username = ProviderAdapterSupport.stringProperty(definition, "username");
            String password = ProviderAdapterSupport.stringProperty(definition, "password");
            DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder();
            if (username != null) {
                clientConfig.user(username);
            }
            if (password != null) {
                clientConfig.password(password);
            }
            UnifiedJedis unifiedJedis = new UnifiedJedis(
                    new HostAndPort(ProviderAdapterSupport.stringProperty(definition, "host"),
                            ProviderAdapterSupport.integerProperty(definition, "port")),
                    clientConfig.build());
            return RedisEmbeddingStore.builder()
                    .unifiedJedis(unifiedJedis)
                    .indexName(ProviderAdapterSupport.stringProperty(definition, "indexName"))
                    .dimension(ProviderAdapterSupport.integerProperty(definition, "dimension"))
                    .metadataKeys(ProviderAdapterSupport.metadataKeys(definition))
                    .build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
