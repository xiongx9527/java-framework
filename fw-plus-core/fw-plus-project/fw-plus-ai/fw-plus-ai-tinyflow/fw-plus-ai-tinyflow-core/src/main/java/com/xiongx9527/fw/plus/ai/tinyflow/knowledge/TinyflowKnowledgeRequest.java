package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import dev.tinyflow.core.chain.Chain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TinyflowKnowledgeRequest {

	private final String providerKey;
	private final String tenantId;
	private final String knowledgeId;
	private final String query;
	private final int topK;
	private final Double threshold;
	private final List<TinyflowMetadataCondition> metadataConditions;
	private final Map<String, Object> metadata;
	private final Map<String, Object> retrievalOptions;
	private final Chain chain;

	private TinyflowKnowledgeRequest(Builder builder) {
		this.providerKey = TinyflowKnowledgeReference.requireText(builder.providerKey, "providerKey");
		this.tenantId = TinyflowKnowledgeReference.requireText(builder.tenantId, "tenantId");
		this.knowledgeId = TinyflowKnowledgeReference.requireText(builder.knowledgeId, "knowledgeId");
		this.query = TinyflowKnowledgeReference.requireText(builder.query, "query");
		if (builder.topK <= 0) {
			throw new IllegalArgumentException("topK must be greater than zero");
		}
		if (builder.threshold != null && (Double.isNaN(builder.threshold.doubleValue())
				|| builder.threshold.doubleValue() < 0D
				|| builder.threshold.doubleValue() > 1D)) {
			throw new IllegalArgumentException("threshold must be between 0 and 1");
		}
		this.topK = builder.topK;
		this.threshold = builder.threshold;
		this.metadataConditions = immutableList(builder.metadataConditions);
		this.metadata = immutableMap(builder.metadata);
		this.retrievalOptions = immutableMap(builder.retrievalOptions);
		this.chain = builder.chain;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static TinyflowKnowledgeRequest from(TinyflowKnowledgeReference reference, String query, int topK,
			Chain chain) {
		if (reference == null) {
			throw new IllegalArgumentException("reference must not be null");
		}
		return builder().providerKey(reference.getProvider()).tenantId(reference.getTenantId())
				.knowledgeId(reference.getKnowledgeId()).query(query).topK(topK).chain(chain).build();
	}

	public String getProviderKey() { return providerKey; }
	public String getTenantId() { return tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public String getQuery() { return query; }
	public int getTopK() { return topK; }
	public Double getThreshold() { return threshold; }
	public List<TinyflowMetadataCondition> getMetadataConditions() { return metadataConditions; }
	public Map<String, Object> getMetadata() { return metadata; }
	public Map<String, Object> getRetrievalOptions() { return retrievalOptions; }
	public Chain getChain() { return chain; }

	static <T> List<T> immutableList(List<T> values) {
		return values == null ? Collections.<T>emptyList()
				: Collections.unmodifiableList(new ArrayList<T>(values));
	}

	static Map<String, Object> immutableMap(Map<String, Object> values) {
		return values == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
	}

	public static final class Builder {
		private String providerKey;
		private String tenantId;
		private String knowledgeId;
		private String query;
		private int topK = 10;
		private Double threshold;
		private List<TinyflowMetadataCondition> metadataConditions;
		private Map<String, Object> metadata;
		private Map<String, Object> retrievalOptions;
		private Chain chain;

		public Builder providerKey(String value) { this.providerKey = value; return this; }
		public Builder tenantId(String value) { this.tenantId = value; return this; }
		public Builder knowledgeId(String value) { this.knowledgeId = value; return this; }
		public Builder query(String value) { this.query = value; return this; }
		public Builder topK(int value) { this.topK = value; return this; }
		public Builder threshold(Double value) { this.threshold = value; return this; }
		public Builder metadataConditions(List<TinyflowMetadataCondition> value) { this.metadataConditions = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public Builder retrievalOptions(Map<String, Object> value) { this.retrievalOptions = value; return this; }
		public Builder chain(Chain value) { this.chain = value; return this; }
		public TinyflowKnowledgeRequest build() { return new TinyflowKnowledgeRequest(this); }
	}
}
