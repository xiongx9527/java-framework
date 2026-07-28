package com.xiongx9527.fw.plus.ai.askdata.vector;

/** 向量检索请求。 */
public final class AskDataVectorSearchRequest {
	private final float[] vector; private final int limit;
	public AskDataVectorSearchRequest(float[] vector, int limit) {
		if (vector == null || limit <= 0) throw new IllegalArgumentException("vector 和 limit 必须有效");
		this.vector = vector.clone(); this.limit = limit;
	}
	public float[] getVector() { return vector.clone(); } public int getLimit() { return limit; }
}
