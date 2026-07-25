package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
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
public class WeaviateModule implements DynamicAttribute<WeaviateModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final WeaviateClient weaviateClient;
    private final LegacyVectorModuleSupport legacySupport;
    private final VectorStoreOperations operations;

    @Deprecated
    public WeaviateModule(Object ignoredDataSource, WeaviateClient weaviateClient, EmbeddingModel embeddingModel) {
        this(weaviateClient, (LegacyVectorModuleSupport) null, null);
    }

    public WeaviateModule(Object ignoredDataSource, WeaviateClient weaviateClient, EmbeddingModel embeddingModel,
                          EmbeddingStoreRegistry registry) {
        this(weaviateClient, new LegacyVectorModuleSupport(registry, "weaviate"), null);
    }

    private WeaviateModule(WeaviateClient weaviateClient, LegacyVectorModuleSupport legacySupport,
                           VectorStoreOperations operations) {
        this.weaviateClient = weaviateClient;
        this.legacySupport = legacySupport;
        this.operations = operations;
    }

    public WeaviateClient getClient() {
        return weaviateClient;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass, Boolean avoidDups, String consistencyLevel,
                                              Collection<String> metadataKeys, String textFieldName) {
        return requireSupport().get(objectClass).getEmbeddingStore();
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass, Collection<String> metadataKeys) {
        return requireSupport().get(objectClass).getEmbeddingStore();
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String objectClass) {
        return requireSupport().get(objectClass).getEmbeddingStore();
    }

    @Override
    public WeaviateModule getDynamicAttribute(String key) {
        return new WeaviateModule(weaviateClient, requireSupport(), requireSupport().get(key));
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return requireSupport().getDefault();
    }

    private LegacyVectorModuleSupport requireSupport() {
        if (legacySupport == null) {
            throw new MagicAPIVectorException("Legacy Magic-API vector provider 'weaviate' is deprecated; migrate script to import vector and access vector['default']. Missing key: default", null);
        }
        return legacySupport;
    }
}
