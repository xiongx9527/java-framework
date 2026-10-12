package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;

/**
 * Milvus 向量库工厂：写法参照 core 层 fw-plus-ai-langchain4j-milvus 的 AutoConfig
 * （Optional 默认值 + @Nullable EmbeddingModel 维度回退）。
 */
public class Langchain4jMilvusModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 19530;
    private static final String DEFAULT_COLLECTION_NAME = "magic_langchain4j";

    @Override
    public String provider() {
        return "milvus";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return MilvusEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .collectionName(o.string("collection-name", DEFAULT_COLLECTION_NAME))
                .dimension(dimension)
                .indexType(o.enumValue("index-type", IndexType.class, IndexType.FLAT))
                .metricType(o.enumValue("metric-type", MetricType.class, MetricType.COSINE))
                .uri(o.string("uri", null))
                .token(o.string("token", null))
                .username(o.string("username", System.getenv("MILVUS_USERNAME")))
                .password(o.string("password", System.getenv("MILVUS_PASSWORD")))
                .consistencyLevel(o.enumValue("consistency-level", ConsistencyLevelEnum.class, ConsistencyLevelEnum.STRONG))
                .retrieveEmbeddingsOnSearch(o.bool("retrieve-embeddings-on-search", null))
                .autoFlushOnInsert(o.bool("auto-flush-on-insert", null))
                .databaseName(o.string("database-name", null))
                .build();
    }
}
