package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;

public class TinyEngineRagRequest {

	private String query;
	private AiEmbeddingVector vector;
	private String collection;
	private String storeKey;
	private int maxResults;
	private double minScore;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	public AiEmbeddingVector getVector() { return vector; }
	public void setVector(AiEmbeddingVector vector) { this.vector = vector; }
	public String getCollection() { return collection; }
	public void setCollection(String collection) { this.collection = collection; }
	public String getStoreKey() { return storeKey; }
	public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
	public int getMaxResults() { return maxResults; }
	public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
	public double getMinScore() { return minScore; }
	public void setMinScore(double minScore) { this.minScore = minScore; }
}
