package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AiEmbeddingQuery {

    private final AiEmbeddingVector vector;

    private final int maxResults;

    private final double minScore;

	private final Map<String, Object> filters;

    public AiEmbeddingQuery(AiEmbeddingVector vector, int maxResults, double minScore) {
		this(vector, maxResults, minScore, Collections.<String, Object>emptyMap());
	}

	public AiEmbeddingQuery(AiEmbeddingVector vector, int maxResults, double minScore,
			Map<String, Object> filters) {
		if (vector == null || vector.dimension() == 0) {
			throw new IllegalArgumentException("vector cannot be empty");
		}
		if (maxResults <= 0) {
			throw new IllegalArgumentException("maxResults must be greater than zero");
		}
		if (Double.isNaN(minScore) || minScore < 0.0D || minScore > 1.0D) {
			throw new IllegalArgumentException("minScore must be between 0 and 1");
		}
		this.vector = vector;
		this.maxResults = maxResults;
		this.minScore = minScore;
		this.filters = filters == null || filters.isEmpty()
				? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(filters));
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

	public Map<String, Object> getFilters() {
		return filters;
	}
}
