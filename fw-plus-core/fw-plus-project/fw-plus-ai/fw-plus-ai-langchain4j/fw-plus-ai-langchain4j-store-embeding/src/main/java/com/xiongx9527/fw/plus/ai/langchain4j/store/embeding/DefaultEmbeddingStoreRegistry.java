package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultEmbeddingStoreRegistry implements EmbeddingStoreRegistry {

    private final Map<String, EmbeddingStore<TextSegment>> stores;
    private final String defaultStore;

    public DefaultEmbeddingStoreRegistry(Map<String, EmbeddingStore<TextSegment>> stores, String defaultStore) {
        this.stores = Collections.unmodifiableMap(new LinkedHashMap<String, EmbeddingStore<TextSegment>>(stores));
        this.defaultStore = defaultStore;
    }

    @Override
    public EmbeddingStore<TextSegment> get(String key) {
        EmbeddingStore<TextSegment> store = stores.get(key);
        if (store == null) {
            throw new EmbeddingStoreException("Embedding store key is not registered: " + key);
        }
        return store;
    }

    @Override
    public EmbeddingStore<TextSegment> getDefault() {
        if (isBlank(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store is not configured");
        }
        if (!stores.containsKey(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store key is invalid: " + defaultStore);
        }
        return stores.get(defaultStore);
    }

    @Override
    public boolean contains(String key) {
        return stores.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return stores.keySet();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
