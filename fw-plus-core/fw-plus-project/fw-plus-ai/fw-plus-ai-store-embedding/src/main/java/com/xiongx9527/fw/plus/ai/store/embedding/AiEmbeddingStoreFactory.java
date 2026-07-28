package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.List;

public interface AiEmbeddingStoreFactory {

    AiEmbeddingStore create(AiEmbeddingStoreDefinition definition, List<AiEmbeddingStoreAdapter> adapters);
}
