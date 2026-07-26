package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

public class InMemoryEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public InMemoryEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore",
                "in-memory", "inmemory", "memory");
    }
}
