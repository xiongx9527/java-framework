package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy.LegacyVectorModuleSupport;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.weaviate.client.WeaviateClient;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Collection;

@Deprecated
@MagicModule("weaviate")
public class WeaviateModule implements DynamicAttribute<VectorStoreOperations, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final WeaviateClient weaviateClient;
    private final LegacyVectorModuleSupport legacySupport;

    @Deprecated
    public WeaviateModule(Object ignoredDataSource, WeaviateClient weaviateClient, EmbeddingModel embeddingModel) {
        this(weaviateClient, new LegacyVectorModuleSupport(null, "weaviate"));
    }

    public WeaviateModule(Object ignoredDataSource, WeaviateClient weaviateClient, EmbeddingModel embeddingModel,
                          EmbeddingStoreRegistry registry) {
        this(weaviateClient, new LegacyVectorModuleSupport(registry, "weaviate"));
    }

    private WeaviateModule(WeaviateClient weaviateClient, LegacyVectorModuleSupport legacySupport) {
        this.weaviateClient = weaviateClient;
        this.legacySupport = legacySupport;
    }

    public WeaviateClient getClient() {
        return weaviateClient;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass, Boolean avoidDups, String consistencyLevel,
                                              Collection<String> metadataKeys, String textFieldName) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("weaviate");
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass, Collection<String> metadataKeys) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("weaviate");
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("weaviate");
    }

    @Override
    public VectorStoreOperations getDynamicAttribute(String key) {
        return legacySupport.get(key);
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return legacySupport.getDefault();
    }
}
