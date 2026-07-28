package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAiEmbeddingStoreRegistry implements AiEmbeddingStoreRegistry {

    private final Map<String, AiEmbeddingStore> stores;

    private final String defaultStoreKey;

    public DefaultAiEmbeddingStoreRegistry(List<AiEmbeddingStoreDefinition> definitions, String defaultStoreKey,
                                           List<AiEmbeddingStoreAdapter> adapters) {
        this(createStores(definitions, adapters), defaultStoreKey);
    }

    public DefaultAiEmbeddingStoreRegistry(Map<String, AiEmbeddingStore> stores, String defaultStoreKey) {
        this(copy(stores), Collections.<String, AiEmbeddingStoreDefinition>emptyMap(), defaultStoreKey);
    }

    private DefaultAiEmbeddingStoreRegistry(BuildResult result, String defaultStoreKey) {
        this(result.stores, result.definitions, defaultStoreKey);
    }

    private DefaultAiEmbeddingStoreRegistry(Map<String, AiEmbeddingStore> stores,
                                            Map<String, AiEmbeddingStoreDefinition> definitions,
                                            String defaultStoreKey) {
        if (!isBlank(defaultStoreKey) && !stores.containsKey(defaultStoreKey)) {
            throw new AiEmbeddingStoreException("Default store key does not exist: "
                    + DefaultAiEmbeddingStoreFactory.context(defaultStoreKey, definitions.get(defaultStoreKey)));
        }
        this.stores = Collections.unmodifiableMap(stores);
        this.defaultStoreKey = defaultStoreKey;
    }

    @Override
    public AiEmbeddingStore get(String key) {
        AiEmbeddingStore store = stores.get(key);
        if (store == null) {
            throw new AiEmbeddingStoreException("Store key does not exist: "
                    + DefaultAiEmbeddingStoreFactory.context(key, null));
        }
        return store;
    }

    @Override
    public AiEmbeddingStore getDefault() {
        if (isBlank(defaultStoreKey)) {
            throw new AiEmbeddingStoreException("Default store key is not configured: "
                    + DefaultAiEmbeddingStoreFactory.context(null, null));
        }
        return get(defaultStoreKey);
    }

    @Override
    public String getDefaultKey() {
        return defaultStoreKey;
    }

    @Override
    public boolean contains(String key) {
        return stores.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(stores.keySet()));
    }

    private static BuildResult createStores(List<AiEmbeddingStoreDefinition> definitions,
                                            List<AiEmbeddingStoreAdapter> adapters) {
        Map<String, AiEmbeddingStoreDefinition> enabled = new LinkedHashMap<String, AiEmbeddingStoreDefinition>();
        Map<String, AiEmbeddingStoreDefinition> all = new LinkedHashMap<String, AiEmbeddingStoreDefinition>();
        if (definitions != null) {
            for (AiEmbeddingStoreDefinition definition : definitions) {
                if (definition == null) {
                    continue;
                }
                String key = definition.getKey();
                if (!all.containsKey(key)) {
                    all.put(key, definition);
                }
                if (!definition.isEnabled()) {
                    continue;
                }
                if (enabled.containsKey(key)) {
                    throw new AiEmbeddingStoreException("Duplicate enabled store key: "
                            + DefaultAiEmbeddingStoreFactory.context(definition));
                }
                enabled.put(key, definition);
            }
        }

        Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
        AiEmbeddingStoreFactory factory = new DefaultAiEmbeddingStoreFactory();
        for (Map.Entry<String, AiEmbeddingStoreDefinition> entry : enabled.entrySet()) {
            stores.put(entry.getKey(), factory.create(entry.getValue(), adapters));
        }
        return new BuildResult(Collections.unmodifiableMap(stores), Collections.unmodifiableMap(all));
    }

    private static Map<String, AiEmbeddingStore> copy(Map<String, AiEmbeddingStore> stores) {
        Map<String, AiEmbeddingStore> copy = new LinkedHashMap<String, AiEmbeddingStore>();
        if (stores != null) {
            copy.putAll(stores);
        }
        return copy;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class BuildResult {

        private final Map<String, AiEmbeddingStore> stores;

        private final Map<String, AiEmbeddingStoreDefinition> definitions;

        private BuildResult(Map<String, AiEmbeddingStore> stores,
                            Map<String, AiEmbeddingStoreDefinition> definitions) {
            this.stores = stores;
            this.definitions = definitions;
        }
    }
}
