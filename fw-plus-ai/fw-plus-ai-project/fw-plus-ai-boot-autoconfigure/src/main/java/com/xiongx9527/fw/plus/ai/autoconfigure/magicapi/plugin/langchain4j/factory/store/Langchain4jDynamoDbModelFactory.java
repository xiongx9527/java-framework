package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.store.embedding.dynamodb.DynamoDbEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * AWS DynamoDB（AOS k-nn 插件风格）向量库工厂：必填 {@code region/region}，常用 {@code table/index-name}。
 * 凭据走 AWS 默认链（环境变量 / ~/.aws 等）。
 */
public class Langchain4jDynamoDbModelFactory implements ModelFactory {

    private static final String DEFAULT_TABLE = "embeddings";
    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";

    @Override
    public String provider() {
        return "dynamodb";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return DynamoDbEmbeddingStore.builder()
                .region(o.required("region"))
                .tableName(o.string("table", DEFAULT_TABLE))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .createTableIfNotExists(o.bool("create-table-if-not-exists", Boolean.TRUE))
                .build();
    }
}
