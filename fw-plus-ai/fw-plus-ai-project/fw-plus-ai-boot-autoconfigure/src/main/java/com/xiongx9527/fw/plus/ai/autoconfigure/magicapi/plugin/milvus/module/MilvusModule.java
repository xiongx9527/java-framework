package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
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
public class MilvusModule implements DynamicAttribute<MilvusModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    private final MilvusClientV2 milvusClientV2;
    private final EmbeddingModel embeddingModel;
    private final LegacyVectorModuleSupport legacySupport;
    private final VectorStoreOperations operations;

    @Deprecated
    public MilvusModule(Object ignoredDataSource, MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel) {
        this(milvusClientV2, embeddingModel, null, null);
    }

    public MilvusModule(Object ignoredDataSource, MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel,
                        EmbeddingStoreRegistry registry) {
        this(milvusClientV2, embeddingModel, new LegacyVectorModuleSupport(registry, "milvus"), null);
    }

    private MilvusModule(MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel,
                         LegacyVectorModuleSupport legacySupport, VectorStoreOperations operations) {
        this.milvusClientV2 = milvusClientV2;
        this.embeddingModel = embeddingModel;
        this.legacySupport = legacySupport;
        this.operations = operations;
    }

    public MilvusClientV2 getClient() {
        return milvusClientV2;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension, IndexType indexType,
                                              MetricType metricType, ConsistencyLevelEnum consistencyLevel,
                                              Boolean retrieveEmbeddingsOnSearch, Boolean autoFlushOnInsert) {
        return resolve(collectionName);
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension) {
        return resolve(collectionName);
    }

    @Override
    public MilvusModule getDynamicAttribute(String key) {
        return new MilvusModule(milvusClientV2, embeddingModel, requireSupport(), requireSupport().get(key));
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return requireSupport().getDefault();
    }

    private EmbeddingStore resolve(String key) {
        return requireSupport().get(key).getEmbeddingStore();
    }

    private LegacyVectorModuleSupport requireSupport() {
        if (legacySupport == null) {
            throw new MagicAPIVectorException("Legacy Magic-API vector provider 'milvus' is deprecated; migrate script to import vector and access vector['default']. Missing key: default", null);
        }
        return legacySupport;
    }
}
