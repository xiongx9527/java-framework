package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class MongoDbEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public MongoDbEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore", "mongodb-atlas", "mongodb");
    }
}
