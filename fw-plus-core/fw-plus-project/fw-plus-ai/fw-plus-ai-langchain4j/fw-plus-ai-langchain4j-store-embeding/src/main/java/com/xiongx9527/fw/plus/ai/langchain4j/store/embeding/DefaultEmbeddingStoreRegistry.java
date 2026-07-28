package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultEmbeddingStoreRegistry implements EmbeddingStoreRegistry {

    private final Map<String, EmbeddingStore<TextSegment>> stores;
    private final String defaultStore;
    private final AiEmbeddingStoreRegistry neutralRegistry;

    public DefaultEmbeddingStoreRegistry(Map<String, EmbeddingStore<TextSegment>> stores, String defaultStore) {
        Map<String, EmbeddingStore<TextSegment>> storeCopy = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        if (stores != null) {
            storeCopy.putAll(stores);
        }
        this.stores = Collections.unmodifiableMap(storeCopy);
        this.defaultStore = defaultStore;
        Map<String, AiEmbeddingStore> neutralStores = new LinkedHashMap<String, AiEmbeddingStore>();
        for (Map.Entry<String, EmbeddingStore<TextSegment>> entry : storeCopy.entrySet()) {
            neutralStores.put(entry.getKey(), neutral(entry.getKey(), entry.getValue()));
        }
        String validDefault = !isBlank(defaultStore) && neutralStores.containsKey(defaultStore) ? defaultStore : null;
        this.neutralRegistry = new DefaultAiEmbeddingStoreRegistry(neutralStores, validDefault);
    }

    @Override
    public EmbeddingStore<TextSegment> get(String key) {
        if (!neutralRegistry.contains(key)) {
            throw new EmbeddingStoreException("Embedding store key is not registered: " + key);
        }
        neutralRegistry.get(key);
        return stores.get(key);
    }

    @Override
    public EmbeddingStore<TextSegment> getDefault() {
        if (isBlank(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store is not configured");
        }
        if (!neutralRegistry.contains(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store key is invalid: " + defaultStore);
        }
        neutralRegistry.getDefault();
        return stores.get(defaultStore);
    }

    @Override
    public boolean contains(String key) {
        return neutralRegistry.contains(key);
    }

    @Override
    public Set<String> keys() {
        return neutralRegistry.keys();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private AiEmbeddingStore neutral(String key, EmbeddingStore<TextSegment> store) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider("langchain4j");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(true);
        return new LangChain4jAiEmbeddingStore(definition, store);
    }
}
