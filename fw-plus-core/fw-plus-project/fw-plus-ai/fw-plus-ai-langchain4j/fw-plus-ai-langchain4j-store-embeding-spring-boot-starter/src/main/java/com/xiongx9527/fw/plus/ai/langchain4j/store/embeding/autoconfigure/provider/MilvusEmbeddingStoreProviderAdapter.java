package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

public class MilvusEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "milvus");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requireOneStringProperty(definition, "host", "uri");
            ProviderAdapterSupport.requiredStringProperty(definition, "collectionName");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            String host = ProviderAdapterSupport.stringProperty(definition, "host");
            if (host != null && host.trim().length() == 0) {
                throw new IllegalArgumentException("host must not be blank");
            }
            return MilvusEmbeddingStore.builder()
                    .host(host)
                    .port(ProviderAdapterSupport.integerProperty(definition, "port"))
                    .uri(ProviderAdapterSupport.stringProperty(definition, "uri"))
                    .collectionName(ProviderAdapterSupport.stringProperty(definition, "collectionName"))
                    .dimension(ProviderAdapterSupport.integerProperty(definition, "dimension"))
                    .token(ProviderAdapterSupport.stringProperty(definition, "apiKey"))
                    .username(ProviderAdapterSupport.stringProperty(definition, "username"))
                    .password(ProviderAdapterSupport.stringProperty(definition, "password"))
                    .build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
