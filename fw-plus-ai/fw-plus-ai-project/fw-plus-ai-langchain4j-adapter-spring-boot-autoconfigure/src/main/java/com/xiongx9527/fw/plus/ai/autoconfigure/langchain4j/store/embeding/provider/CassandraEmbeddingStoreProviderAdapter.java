package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class CassandraEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public CassandraEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.cassandra.CassandraEmbeddingStore", "cassandra");
    }
}
