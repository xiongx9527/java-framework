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

    /**
     * Removes the requested ids idempotently. Implementations must treat duplicate or already missing ids as
     * successfully removed so callers can safely retry a durable compensation after an uncertain result.
     */
    void removeAll(List<String> ids);

	default boolean supportsFilters() {
		return false;
	}

    AiEmbeddingSearchResult search(AiEmbeddingQuery query);
}
