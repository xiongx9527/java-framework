package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import java.util.List;

public interface TinyflowVectorStore {

	List<String> addAll(List<TinyflowVectorDocument> documents);

	void removeAll(List<String> ids);

	List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore);
}
