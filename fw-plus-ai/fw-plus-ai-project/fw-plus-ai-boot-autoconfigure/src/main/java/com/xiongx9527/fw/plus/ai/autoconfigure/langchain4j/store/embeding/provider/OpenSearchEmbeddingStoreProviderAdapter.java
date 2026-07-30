package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class OpenSearchEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public OpenSearchEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore", "opensearch");
    }
}
