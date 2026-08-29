package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class PgVectorEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public PgVectorEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore",
                "pgvector", "postgres", "postgresql");
    }
}
