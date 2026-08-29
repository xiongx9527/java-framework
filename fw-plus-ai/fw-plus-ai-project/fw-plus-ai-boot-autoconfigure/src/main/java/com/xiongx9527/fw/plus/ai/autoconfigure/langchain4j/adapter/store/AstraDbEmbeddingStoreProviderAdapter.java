package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class AstraDbEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public AstraDbEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.astradb.AstraDbEmbeddingStore", "astra-db");
    }
}
