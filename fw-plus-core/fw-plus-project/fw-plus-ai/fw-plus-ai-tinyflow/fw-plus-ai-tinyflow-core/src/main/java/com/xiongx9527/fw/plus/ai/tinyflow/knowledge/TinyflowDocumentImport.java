package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public final class TinyflowDocumentImport {

	private final String providerKey;
	private final String tenantId;
	private final String knowledgeId;
	private final TinyflowDocument document;
	private final Map<String, Object> options;

	private TinyflowDocumentImport(Builder builder) {
		this.providerKey = TinyflowKnowledgeReference.requireText(builder.providerKey, "providerKey");
		this.tenantId = TinyflowKnowledgeReference.requireText(builder.tenantId, "tenantId");
		this.knowledgeId = TinyflowKnowledgeReference.requireText(builder.knowledgeId, "knowledgeId");
		if (builder.document == null) {
			throw new IllegalArgumentException("document must not be null");
		}
		this.document = builder.document;
		this.options = TinyflowKnowledgeRequest.immutableMap(builder.options);
	}

	public static Builder builder() { return new Builder(); }
	public String getProviderKey() { return providerKey; }
	public String getTenantId() { return tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public TinyflowDocument getDocument() { return document; }
	public Map<String, Object> getOptions() { return options; }

	public static final class Builder {
		private String providerKey;
		private String tenantId;
		private String knowledgeId;
		private TinyflowDocument document;
		private Map<String, Object> options;
		public Builder providerKey(String value) { this.providerKey = value; return this; }
		public Builder tenantId(String value) { this.tenantId = value; return this; }
		public Builder knowledgeId(String value) { this.knowledgeId = value; return this; }
		public Builder document(TinyflowDocument value) { this.document = value; return this; }
		public Builder options(Map<String, Object> value) { this.options = value; return this; }
		public TinyflowDocumentImport build() { return new TinyflowDocumentImport(this); }
	}
}
