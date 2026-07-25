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
public class VectorModule implements DynamicAttribute<VectorModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final EmbeddingStoreRegistry registry;
    private final VectorStoreOperations operations;

    public VectorModule(EmbeddingStoreRegistry registry) {
        this(registry, null);
    }

    private VectorModule(EmbeddingStoreRegistry registry, VectorStoreOperations operations) {
        this.registry = registry;
        this.operations = operations;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    @Override
    public VectorModule getDynamicAttribute(String key) {
        try {
            return new VectorModule(registry, operations(registry.get(key)));
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
