package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import java.util.Collections;
import java.util.List;

public interface TinyflowVectorStore {

	default String add(TinyflowVectorDocument document) {
		List<String> ids = addAll(Collections.singletonList(document));
		if (ids == null || ids.size() != 1 || ids.get(0) == null || ids.get(0).trim().isEmpty()) {
			throw new IllegalStateException("Vector store returned an invalid vector id");
		}
		return ids.get(0);
	}

	List<String> addAll(List<TinyflowVectorDocument> documents);

	/**
	 * Removes the requested vector ids idempotently. Duplicate or already missing ids must be
	 * treated as successfully removed so a durable compensation can be retried after an uncertain result.
	 */
	void removeAll(List<String> ids);

	List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore);
}
