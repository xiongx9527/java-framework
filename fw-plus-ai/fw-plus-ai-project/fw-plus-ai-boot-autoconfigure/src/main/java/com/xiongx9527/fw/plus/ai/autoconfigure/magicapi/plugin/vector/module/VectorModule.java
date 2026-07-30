package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("vector")
public class VectorModule implements DynamicAttribute<VectorModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final VectorStoreResolver resolver;
    private final VectorStoreOperations operations;

    public VectorModule(AiEmbeddingStoreRegistry registry) {
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

    public AiEmbeddingStore getEmbeddingStore() {
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

    private VectorStoreOperations operations(AiEmbeddingStore embeddingStore) {
        return new VectorStoreOperations(embeddingStore);
    }

    private static class SpringBootVectorStoreResolver implements VectorStoreResolver {

        private final AiEmbeddingStoreRegistry registry;

        private SpringBootVectorStoreResolver(AiEmbeddingStoreRegistry registry) {
            this.registry = registry;
        }

        @Override
        public AiEmbeddingStore get(String key) {
            return registry.get(key);
        }

        @Override
        public AiEmbeddingStore getDefault() {
            return registry.getDefault();
        }
    }
}
