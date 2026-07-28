package com.xiongx9527.fw.plus.ai.store.embedding;

public final class AiEmbeddingQuery {

    private final AiEmbeddingVector vector;

    private final int maxResults;

    private final double minScore;

    public AiEmbeddingQuery(AiEmbeddingVector vector, int maxResults, double minScore) {
        this.vector = vector;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public AiEmbeddingVector getVector() {
        return vector;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public double getMinScore() {
        return minScore;
    }
}
