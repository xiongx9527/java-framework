package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class CassandraEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public CassandraEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.cassandra.CassandraEmbeddingStore", "cassandra");
    }
}
