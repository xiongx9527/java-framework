package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy.LegacyVectorModuleSupport;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.qdrant.client.QdrantClient;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Deprecated
@MagicModule("qdrant")
public class QdrantModule implements DynamicAttribute<QdrantModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final QdrantClient qdrantClient;
    private final LegacyVectorModuleSupport legacySupport;
    private final VectorStoreOperations operations;

    @Deprecated
    public QdrantModule(Object ignoredDataSource, QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this(qdrantClient, (LegacyVectorModuleSupport) null, null);
    }

    public QdrantModule(Object ignoredDataSource, QdrantClient qdrantClient, EmbeddingModel embeddingModel,
                        EmbeddingStoreRegistry registry) {
        this(qdrantClient, new LegacyVectorModuleSupport(registry, "qdrant"), null);
    }

    private QdrantModule(QdrantClient qdrantClient, LegacyVectorModuleSupport legacySupport,
                         VectorStoreOperations operations) {
        this.qdrantClient = qdrantClient;
        this.legacySupport = legacySupport;
        this.operations = operations;
    }

    public QdrantClient getClient() {
        return qdrantClient;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, String payloadTextKey) {
        return requireSupport().get(collectionName).getEmbeddingStore();
    }

    @Override
    public QdrantModule getDynamicAttribute(String key) {
        return new QdrantModule(qdrantClient, requireSupport(), requireSupport().get(key));
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return requireSupport().getDefault();
    }

    private LegacyVectorModuleSupport requireSupport() {
        if (legacySupport == null) {
            throw new MagicAPIVectorException("Legacy Magic-API vector provider 'qdrant' is deprecated; migrate script to import vector and access vector['default']. Missing key: default", null);
        }
        return legacySupport;
    }
}
