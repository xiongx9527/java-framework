package com.xiongx9527.fw.plus.ai.askdata.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 向量检索请求。 */
public final class AskDataVectorSearchRequest {
	private final float[] vector; private final int limit; private final Map<String, String> filters;
	public AskDataVectorSearchRequest(float[] vector, int limit) {
		this(vector, limit, Collections.<String, String>emptyMap());
	}
	public AskDataVectorSearchRequest(float[] vector, int limit, Map<String, String> filters) {
		if (vector == null || limit <= 0) throw new IllegalArgumentException("vector 和 limit 必须有效");
		this.vector = vector.clone(); this.limit = limit;
		this.filters = filters == null ? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(filters));
	}
	public float[] getVector() { return vector.clone(); } public int getLimit() { return limit; }
	public Map<String, String> getFilters() { return filters; }
}
