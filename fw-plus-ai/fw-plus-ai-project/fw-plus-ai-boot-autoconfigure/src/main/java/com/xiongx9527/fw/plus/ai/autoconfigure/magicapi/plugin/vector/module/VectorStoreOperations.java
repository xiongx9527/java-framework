package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class VectorStoreOperations {

    private final EmbeddingStore<TextSegment> embeddingStore;

    public VectorStoreOperations(EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }
}
