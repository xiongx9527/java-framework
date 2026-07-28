package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.List;

public interface AiEmbeddingStore {

    String getKey();

    String getFramework();

    String getProvider();

    AiEmbeddingStoreType getType();

    String add(AiEmbeddingDocument document);

    List<String> addAll(List<AiEmbeddingDocument> documents);

    void remove(String id);

    void removeAll(List<String> ids);

    AiEmbeddingSearchResult search(AiEmbeddingQuery query);
}
