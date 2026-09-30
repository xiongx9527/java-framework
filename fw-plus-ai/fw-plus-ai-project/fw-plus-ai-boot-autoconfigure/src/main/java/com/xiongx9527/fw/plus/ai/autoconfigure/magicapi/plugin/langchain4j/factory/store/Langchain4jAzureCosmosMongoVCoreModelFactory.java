package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.azure.cosmos.mongo.vcore.AzureCosmosDbMongoVCoreEmbeddingStore;

/**
 * Azure Cosmos DB（Mongo vCore）向量库工厂。
 * 必填 {@code connection-string}；常用 {@code database/collection/dimension}。
 */
public class Langchain4jAzureCosmosMongoVCoreModelFactory implements ModelFactory {

    private static final String DEFAULT_DATABASE = "default";
    private static final String DEFAULT_COLLECTION = "embeddings";

    @Override
    public String provider() {
        return "azure-cosmos-mongo-vcore";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return AzureCosmosDbMongoVCoreEmbeddingStore.builder()
                .connectionString(o.required("connection-string"))
                .databaseName(o.string("database", DEFAULT_DATABASE))
                .collectionName(o.string("collection", DEFAULT_COLLECTION))
                .indexName(o.string("index-name", "default"))
                .dimensions(dimension)
                .numLists(o.integer("num-lists", null))
                .efConstruction(o.integer("ef-construction", null))
                .build();
    }
}
