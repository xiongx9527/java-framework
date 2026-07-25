package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class LegacyVectorModuleSupport {

    private final EmbeddingStoreRegistry registry;
    private final String provider;

    public LegacyVectorModuleSupport(EmbeddingStoreRegistry registry, String provider) {
        this.registry = registry;
        this.provider = provider;
    }

    public VectorStoreOperations get(String key) {
        try {
            return operations(registry.get(key));
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException(migrationMessage(key), e);
        }
    }

    public VectorStoreOperations getDefault() {
        try {
            return operations(registry.getDefault());
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException(migrationMessage("default"), e);
        }
    }

    private VectorStoreOperations operations(EmbeddingStore<TextSegment> embeddingStore) {
        return new VectorStoreOperations(embeddingStore);
    }

    private String migrationMessage(String key) {
        return "Legacy Magic-API vector provider '" + provider + "' is deprecated; migrate script to import vector and access vector['"
                + key + "']. Missing key: " + key;
    }
}
