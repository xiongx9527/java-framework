package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy.LegacyVectorModuleSupport;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.v2.client.MilvusClientV2;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Deprecated
@MagicModule("milvus")
public class MilvusModule implements DynamicAttribute<VectorStoreOperations, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final MilvusClientV2 milvusClientV2;
    private final EmbeddingModel embeddingModel;
    private final LegacyVectorModuleSupport legacySupport;

    @Deprecated
    public MilvusModule(Object ignoredDataSource, MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel) {
        this(milvusClientV2, embeddingModel, new LegacyVectorModuleSupport(null, "milvus"));
    }

    public MilvusModule(Object ignoredDataSource, MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel,
                        EmbeddingStoreRegistry registry) {
        this(milvusClientV2, embeddingModel, new LegacyVectorModuleSupport(registry, "milvus"));
    }

    private MilvusModule(MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel,
                         LegacyVectorModuleSupport legacySupport) {
        this.milvusClientV2 = milvusClientV2;
        this.embeddingModel = embeddingModel;
        this.legacySupport = legacySupport;
    }

    public MilvusClientV2 getClient() {
        return milvusClientV2;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension, IndexType indexType,
                                              MetricType metricType, ConsistencyLevelEnum consistencyLevel,
                                              Boolean retrieveEmbeddingsOnSearch, Boolean autoFlushOnInsert) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("milvus");
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("milvus");
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
