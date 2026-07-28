package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Set;

public interface AiEmbeddingStoreRegistry {

    AiEmbeddingStore get(String key);

    AiEmbeddingStore getDefault();

    String getDefaultKey();

    boolean contains(String key);

    Set<String> keys();
}
