package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;

public class WeaviateEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "weaviate");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "scheme");
            ProviderAdapterSupport.requiredStringProperty(definition, "host");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "port");
            ProviderAdapterSupport.requiredStringProperty(definition, "className");
            return WeaviateEmbeddingStore.builder()
                    .apiKey(ProviderAdapterSupport.stringProperty(definition, "apiKey"))
                    .scheme(ProviderAdapterSupport.stringProperty(definition, "scheme"))
                    .host(ProviderAdapterSupport.stringProperty(definition, "host"))
                    .port(ProviderAdapterSupport.integerProperty(definition, "port"))
                    .grpcPort(ProviderAdapterSupport.integerProperty(definition, "grpcPort"))
                    .objectClass(ProviderAdapterSupport.stringProperty(definition, "className"))
                    .metadataKeys(ProviderAdapterSupport.metadataKeys(definition))
                    .build();
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
