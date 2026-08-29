package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

public class ChromaEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    public ChromaEmbeddingStoreProviderAdapter() {
        super("dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore", "chroma");
    }
}
