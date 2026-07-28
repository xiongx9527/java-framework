package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;

public class VectorStoreOperations {

    private final AiEmbeddingStore embeddingStore;

    public VectorStoreOperations(AiEmbeddingStore embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    public AiEmbeddingStore getEmbeddingStore() {
        return embeddingStore;
    }
}
