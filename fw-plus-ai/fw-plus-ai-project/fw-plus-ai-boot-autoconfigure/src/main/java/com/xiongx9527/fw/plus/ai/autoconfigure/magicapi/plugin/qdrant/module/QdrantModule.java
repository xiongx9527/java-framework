package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.module;

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
public class QdrantModule implements DynamicAttribute<VectorStoreOperations, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final QdrantClient qdrantClient;
    private final LegacyVectorModuleSupport legacySupport;

    @Deprecated
    public QdrantModule(Object ignoredDataSource, QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this(qdrantClient, new LegacyVectorModuleSupport(null, "qdrant"));
    }

    public QdrantModule(Object ignoredDataSource, QdrantClient qdrantClient, EmbeddingModel embeddingModel,
                        EmbeddingStoreRegistry registry) {
        this(qdrantClient, new LegacyVectorModuleSupport(registry, "qdrant"));
    }

    private QdrantModule(QdrantClient qdrantClient, LegacyVectorModuleSupport legacySupport) {
        this.qdrantClient = qdrantClient;
        this.legacySupport = legacySupport;
    }

    public QdrantClient getClient() {
        return qdrantClient;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, String payloadTextKey) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("qdrant");
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
