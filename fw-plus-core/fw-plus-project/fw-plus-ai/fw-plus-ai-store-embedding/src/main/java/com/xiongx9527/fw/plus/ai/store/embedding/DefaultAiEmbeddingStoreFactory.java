package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.List;

public class DefaultAiEmbeddingStoreFactory implements AiEmbeddingStoreFactory {

    @Override
    public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition, List<AiEmbeddingStoreAdapter> adapters) {
        validateDefinition(definition);
        if (adapters != null) {
            for (AiEmbeddingStoreAdapter adapter : adapters) {
                if (adapter != null && supports(adapter, definition)) {
                    return create(adapter, definition);
                }
            }
        }
        throw new AiEmbeddingStoreException("No adapter supports store: " + context(definition));
    }

    private void validateDefinition(AiEmbeddingStoreDefinition definition) {
        if (definition == null) {
            throw new AiEmbeddingStoreException("Store definition must not be null: " + context(null));
        }
        if (isBlank(definition.getKey())) {
            throw new AiEmbeddingStoreException("Store definition key must not be blank: " + context(definition));
        }
        if (isBlank(definition.getFramework())) {
            throw new AiEmbeddingStoreException("Store definition framework must not be blank: " + context(definition));
        }
        if (isBlank(definition.getProvider())) {
            throw new AiEmbeddingStoreException("Store definition provider must not be blank: " + context(definition));
        }
        if (definition.getType() == null) {
            throw new AiEmbeddingStoreException("Store definition type must not be null: " + context(definition));
        }
    }

    private boolean supports(AiEmbeddingStoreAdapter adapter, AiEmbeddingStoreDefinition definition) {
        try {
            return adapter.supports(definition);
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("Adapter failed while checking store support: "
                    + context(definition) + ", causeType=" + e.getClass().getName());
        }
    }

    private AiEmbeddingStore create(AiEmbeddingStoreAdapter adapter, AiEmbeddingStoreDefinition definition) {
        AiEmbeddingStore store;
        try {
            store = adapter.create(definition);
        } catch (Exception e) {
            throw new AiEmbeddingStoreException("Adapter failed to create store: "
                    + context(definition) + ", causeType=" + e.getClass().getName());
        }
        if (store == null) {
            throw new AiEmbeddingStoreException("Adapter returned null store: " + context(definition));
        }
        return store;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static String context(AiEmbeddingStoreDefinition definition) {
        return context(definition == null ? null : definition.getKey(), definition);
    }

    static String context(String key, AiEmbeddingStoreDefinition definition) {
        return "key=" + value(key)
                + ", framework=" + value(definition == null ? null : definition.getFramework())
                + ", provider=" + value(definition == null ? null : definition.getProvider())
                + ", type=" + value(definition == null ? null : definition.getType());
    }

    private static String value(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }
}
