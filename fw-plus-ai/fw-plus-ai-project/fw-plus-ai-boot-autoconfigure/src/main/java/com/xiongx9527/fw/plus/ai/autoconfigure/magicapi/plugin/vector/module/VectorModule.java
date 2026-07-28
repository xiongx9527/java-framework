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

    /** @deprecated 生产自动配置已改用中立 registry，仅保留旧源码的构造兼容。 */
    @Deprecated
    public VectorModule(final com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry registry) {
        this(new VectorStoreResolver() {
            @Override public AiEmbeddingStore get(String key) {
                return legacy(key, registry.get(key));
            }
            @Override public AiEmbeddingStore getDefault() {
                return legacy("default", registry.getDefault());
            }
        }, null);
    }

    private static AiEmbeddingStore legacy(final String key,
            final dev.langchain4j.store.embedding.EmbeddingStore<dev.langchain4j.data.segment.TextSegment> store) {
        return new AiEmbeddingStore() {
            @Override public String getKey() { return key; }
            @Override public String getFramework() { return "langchain4j"; }
            @Override public String getProvider() { return "langchain4j"; }
            @Override public com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType getType() {
                return com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType.VECTOR;
            }
            @Override public String add(com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument document) { throw unsupported(); }
            @Override public java.util.List<String> addAll(java.util.List<com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument> documents) { throw unsupported(); }
            @Override public void remove(String id) { store.remove(id); }
            @Override public void removeAll(java.util.List<String> ids) { store.removeAll(ids); }
            @Override public com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult search(
                    com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery query) { throw unsupported(); }
            private com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException unsupported() {
                return new com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException(
                        "Legacy constructor facade does not expose native operations");
            }
        };
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
