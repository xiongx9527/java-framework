package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public class DefaultEmbeddingStoreFactory implements EmbeddingStoreFactory {

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition, List<EmbeddingStoreProviderAdapter> adapters) {
        if (definition == null) {
            throw new EmbeddingStoreException("Embedding store definition must not be null");
        }
        EmbeddingStoreProviderAdapter adapter;
        try {
            adapter = findAdapter(definition.getProvider(), adapters);
        } catch (RuntimeException e) {
            throw new EmbeddingStoreException("Failed to create embedding store for provider: "
                    + definition.getProvider() + ", key: " + definition.getKey(), e);
        }
        if (adapter == null) {
            throw new EmbeddingStoreException("Embedding store provider is not supported: " + definition.getProvider());
        }
        try {
            EmbeddingStore<TextSegment> store = adapter.create(definition);
            if (store == null) {
                throw new IllegalStateException("Adapter returned null embedding store");
            }
            return store;
        } catch (RuntimeException e) {
            throw new EmbeddingStoreException("Failed to create embedding store for provider: "
                    + definition.getProvider() + ", key: " + definition.getKey(), e);
        }
    }

    private EmbeddingStoreProviderAdapter findAdapter(String provider, List<EmbeddingStoreProviderAdapter> adapters) {
        if (adapters == null) {
            return null;
        }
        for (EmbeddingStoreProviderAdapter adapter : adapters) {
            if (adapter != null && adapter.supports(provider)) {
                return adapter;
            }
        }
        return null;
    }
}
