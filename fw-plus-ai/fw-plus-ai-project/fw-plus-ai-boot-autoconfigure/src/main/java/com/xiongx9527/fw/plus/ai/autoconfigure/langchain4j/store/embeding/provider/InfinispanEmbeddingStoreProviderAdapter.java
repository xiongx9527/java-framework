package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Collections;

public class InfinispanEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private final LangChain4jEmbeddingStoreProviderAdapter delegate =
            new LangChain4jEmbeddingStoreProviderAdapter(Collections.singletonMap("infinispan",
                    "dev.langchain4j.store.embedding.infinispan.InfinispanEmbeddingStore"));

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "infinispan");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            ProviderAdapterSupport.requiredStringProperty(definition, "cacheName");
            ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
            return delegate.create(definition);
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }
}
