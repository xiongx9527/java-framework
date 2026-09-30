package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.store.embedding.oceanbase.OceanBaseEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * OceanBase（4.3+ VECTOR 列）向量库工厂：必填 {@code url}，常用 {@code user/password/table/dimension}。
 */
public class Langchain4jOceanBaseModelFactory implements ModelFactory {

    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "oceanbase";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return OceanBaseEmbeddingStore.builder()
                .url(o.required("url"))
                .user(o.string("user", null))
                .password(o.string("password", null))
                .tableName(o.string("table", DEFAULT_TABLE))
                .dimension(dimension)
                .enableHybridSearch(o.bool("hybrid-search", Boolean.FALSE))
                .build();
    }
}
