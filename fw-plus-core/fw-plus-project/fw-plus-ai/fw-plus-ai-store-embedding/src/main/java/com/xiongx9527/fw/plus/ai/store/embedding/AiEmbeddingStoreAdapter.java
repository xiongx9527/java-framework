package com.xiongx9527.fw.plus.ai.store.embedding;

public interface AiEmbeddingStoreAdapter {

    boolean supports(AiEmbeddingStoreDefinition definition);

    AiEmbeddingStore create(AiEmbeddingStoreDefinition definition);
}
