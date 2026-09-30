package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;

/**
 * MongoDB Atlas（Atlas Vector Search）向量库工厂。
 * 必填 {@code connection-string}；常用 {@code database/collection/index-name}。
 */
public class Langchain4jMongoDbAtlasModelFactory implements ModelFactory {

    private static final String DEFAULT_DATABASE = "default";
    private static final String DEFAULT_COLLECTION = "embeddings";

    @Override
    public String provider() {
        return "mongodb-atlas";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        MongoClient client = MongoClients.create(o.required("connection-string"));
        return MongoDbEmbeddingStore.builder()
                .fromClient(client)
                .databaseName(o.string("database", DEFAULT_DATABASE))
                .collectionName(o.string("collection", DEFAULT_COLLECTION))
                .indexName(o.string("index-name", "default"))
                .createIndex(o.bool("create-index", Boolean.TRUE))
                .build();
    }
}
