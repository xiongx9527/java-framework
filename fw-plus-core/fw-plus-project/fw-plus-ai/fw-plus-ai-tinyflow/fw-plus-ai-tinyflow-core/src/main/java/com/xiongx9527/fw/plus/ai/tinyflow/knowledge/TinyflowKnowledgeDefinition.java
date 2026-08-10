package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public final class TinyflowKnowledgeDefinition {

	private final String providerKey;
	private final String tenantId;
	private final String knowledgeId;
	private final String name;
	private final String description;
	private final Map<String, Object> metadata;
	private final boolean enabled;

	private TinyflowKnowledgeDefinition(Builder builder) {
		this.providerKey = TinyflowKnowledgeReference.requireText(builder.providerKey, "providerKey");
		this.tenantId = TinyflowKnowledgeReference.requireText(builder.tenantId, "tenantId");
		this.knowledgeId = TinyflowKnowledgeReference.requireText(builder.knowledgeId, "knowledgeId");
		this.name = TinyflowKnowledgeReference.requireText(builder.name, "name");
		this.description = builder.description;
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
		this.enabled = builder.enabled;
	}

	public static Builder builder() { return new Builder(); }
	public String getProviderKey() { return providerKey; }
	public String getTenantId() { return tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public Map<String, Object> getMetadata() { return metadata; }
	public boolean isEnabled() { return enabled; }

	public static final class Builder {
		private String providerKey;
		private String tenantId;
		private String knowledgeId;
		private String name;
		private String description;
		private Map<String, Object> metadata;
		private boolean enabled = true;
		public Builder providerKey(String value) { this.providerKey = value; return this; }
		public Builder tenantId(String value) { this.tenantId = value; return this; }
		public Builder knowledgeId(String value) { this.knowledgeId = value; return this; }
		public Builder name(String value) { this.name = value; return this; }
		public Builder description(String value) { this.description = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public Builder enabled(boolean value) { this.enabled = value; return this; }
		public TinyflowKnowledgeDefinition build() { return new TinyflowKnowledgeDefinition(this); }
	}
}
