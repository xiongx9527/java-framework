package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;

public interface VectorStoreResolver {

    AiEmbeddingStore get(String key);

    AiEmbeddingStore getDefault();
}
