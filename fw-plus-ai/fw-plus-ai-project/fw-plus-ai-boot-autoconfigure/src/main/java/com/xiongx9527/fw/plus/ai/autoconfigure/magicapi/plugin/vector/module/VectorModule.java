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

    private final VectorStoreResolver resolver;
    private final VectorStoreOperations operations;

    public VectorModule(EmbeddingStoreRegistry registry) {
        this(new SpringBootVectorStoreResolver(registry), null);
    }

    public VectorModule(VectorStoreResolver resolver) {
        this(resolver, null);
    }

    private VectorModule(VectorStoreResolver resolver, VectorStoreOperations operations) {
        this.resolver = resolver;
        this.operations = operations;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        if (operations != null) {
            return operations.getEmbeddingStore();
        }
        return resolver.getDefault();
    }

    @Override
    public VectorModule getDynamicAttribute(String key) {
        try {
            return new VectorModule(resolver, operations(resolver.get(key)));
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException("Unable to resolve vector store for key '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        try {
            return operations(resolver.getDefault());
        } catch (RuntimeException e) {
            throw new MagicAPIVectorException("Unable to resolve default vector store: " + e.getMessage(), e);
        }
    }

    private VectorStoreOperations operations(EmbeddingStore<TextSegment> embeddingStore) {
        return new VectorStoreOperations(embeddingStore);
    }

    private static class SpringBootVectorStoreResolver implements VectorStoreResolver {

        private final EmbeddingStoreRegistry registry;

        private SpringBootVectorStoreResolver(EmbeddingStoreRegistry registry) {
            this.registry = registry;
        }

        @Override
        public EmbeddingStore<TextSegment> get(String key) {
            return registry.get(key);
        }

        @Override
        public EmbeddingStore<TextSegment> getDefault() {
            return registry.getDefault();
        }
    }
}
