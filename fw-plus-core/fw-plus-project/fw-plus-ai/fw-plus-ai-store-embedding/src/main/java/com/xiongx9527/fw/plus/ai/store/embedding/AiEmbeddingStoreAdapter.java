package com.xiongx9527.fw.plus.ai.store.embedding;

public interface AiEmbeddingStoreAdapter {

    String getFramework();

    boolean supports(AiEmbeddingStoreDefinition definition);

    AiEmbeddingStore create(AiEmbeddingStoreDefinition definition);
}
