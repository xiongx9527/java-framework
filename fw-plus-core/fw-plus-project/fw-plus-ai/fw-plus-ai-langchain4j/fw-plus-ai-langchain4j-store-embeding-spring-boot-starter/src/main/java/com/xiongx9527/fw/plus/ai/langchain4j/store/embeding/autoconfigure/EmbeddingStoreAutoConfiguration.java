package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.DefaultEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.DefaultEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider.ElasticsearchEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider.MilvusEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider.QdrantEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider.RedisStackEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider.WeaviateEmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(EmbeddingStoreProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.embedding-store", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingStoreAutoConfiguration {

    private static final String PREFIX = "fw-plus.ai.langchain4j.embedding-store";

    @Bean
    @ConditionalOnMissingBean(EmbeddingStoreFactory.class)
    public EmbeddingStoreFactory embeddingStoreFactory() {
        return new DefaultEmbeddingStoreFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisStackEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter redisStackEmbeddingStoreProviderAdapter() {
        return new RedisStackEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "milvusEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter milvusEmbeddingStoreProviderAdapter() {
        return new MilvusEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "elasticsearchEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter elasticsearchEmbeddingStoreProviderAdapter() {
        return new ElasticsearchEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "qdrantEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter qdrantEmbeddingStoreProviderAdapter() {
        return new QdrantEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "weaviateEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter weaviateEmbeddingStoreProviderAdapter() {
        return new WeaviateEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingStoreRegistry.class)
    public EmbeddingStoreRegistry embeddingStoreRegistry(EmbeddingStoreProperties properties,
                                                         EmbeddingStoreFactory factory,
                                                         List<EmbeddingStoreProviderAdapter> adapters) {
        Map<String, EmbeddingStoreDefinition> definitions = definitions(properties);
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        for (EmbeddingStoreDefinition definition : definitions.values()) {
            stores.put(definition.getKey(), factory.create(definition, adapters));
        }
        validateDefaultStore(properties.getDefaultStore(), stores);
        return new DefaultEmbeddingStoreRegistry(stores, properties.getDefaultStore());
    }

    private Map<String, EmbeddingStoreDefinition> definitions(EmbeddingStoreProperties properties) {
        Map<String, EmbeddingStoreDefinition> definitions = new LinkedHashMap<String, EmbeddingStoreDefinition>();
        for (Map.Entry<String, EmbeddingStoreDefinition> entry : properties.getStores().entrySet()) {
            EmbeddingStoreDefinition definition = entry.getValue();
            if (definition == null || !definition.isEnabled()) {
                continue;
            }
            if (isBlank(definition.getKey())) {
                definition.setKey(entry.getKey());
            }
            if (isBlank(definition.getKey())) {
                throw new EmbeddingStoreException("Embedding store key must not be blank");
            }
            if (isBlank(definition.getProvider())) {
                throw new EmbeddingStoreException("Embedding store provider must not be blank: " + definition.getKey());
            }
            if (definitions.put(definition.getKey(), definition) != null) {
                throw new EmbeddingStoreException("Duplicate embedding store key: " + definition.getKey());
            }
        }
        return definitions;
    }

    private void validateDefaultStore(String defaultStore, Map<String, EmbeddingStore<TextSegment>> stores) {
        if (!isBlank(defaultStore) && !stores.containsKey(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store key is invalid: " + defaultStore);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
