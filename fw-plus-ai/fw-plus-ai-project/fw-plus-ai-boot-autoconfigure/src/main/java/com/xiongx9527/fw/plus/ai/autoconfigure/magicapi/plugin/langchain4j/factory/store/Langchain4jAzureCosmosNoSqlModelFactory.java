package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.azure.cosmos.nosql.AzureCosmosDbNoSqlEmbeddingStore;

/**
 * Azure Cosmos DB（NoSql）向量库工厂。
 * 必填 {@code endpoint/api-key}；常用 {@code database/container/partition-key-path}。
 */
public class Langchain4jAzureCosmosNoSqlModelFactory implements ModelFactory {

    private static final String DEFAULT_DATABASE = "default";
    private static final String DEFAULT_CONTAINER = "embeddings";

    @Override
    public String provider() {
        return "azure-cosmos-nosql";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return AzureCosmosDbNoSqlEmbeddingStore.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .databaseName(o.string("database", DEFAULT_DATABASE))
                .containerName(o.string("container", DEFAULT_CONTAINER))
                .partitionKeyPath(o.string("partition-key-path", "/partitionKey"))
                .build();
    }
}
