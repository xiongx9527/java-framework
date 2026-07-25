package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("vector")
public class VectorModule implements DynamicAttribute<VectorStoreOperations, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final EmbeddingStoreRegistry registry;

    public VectorModule(EmbeddingStoreRegistry registry) {
        this.registry = registry;
    }

    @Override
    public VectorStoreOperations getDynamicAttribute(String key) {
        try {
            return operations(registry.get(key));
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException("Unable to resolve vector store for key '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        try {
            return operations(registry.getDefault());
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException("Unable to resolve default vector store: " + e.getMessage(), e);
        }
    }

    private VectorStoreOperations operations(EmbeddingStore<TextSegment> embeddingStore) {
        return new VectorStoreOperations(embeddingStore);
    }
}
