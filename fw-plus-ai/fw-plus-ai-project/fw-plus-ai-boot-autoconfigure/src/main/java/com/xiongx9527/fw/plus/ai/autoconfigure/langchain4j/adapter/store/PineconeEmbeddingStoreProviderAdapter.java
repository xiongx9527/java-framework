package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class PineconeEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public PineconeEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore", "pinecone");
    }
}
