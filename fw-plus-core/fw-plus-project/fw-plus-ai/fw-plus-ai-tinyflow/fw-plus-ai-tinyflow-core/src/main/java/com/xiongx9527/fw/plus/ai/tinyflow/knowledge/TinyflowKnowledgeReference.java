package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public final class TinyflowKnowledgeReference {

	private final String provider;
	private final String tenantId;
	private final String knowledgeId;

	private TinyflowKnowledgeReference(Builder builder) {
		this.provider = requireText(builder.provider, "provider");
		this.tenantId = requireText(builder.tenantId, "tenantId");
		this.knowledgeId = requireText(builder.knowledgeId, "knowledgeId");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static TinyflowKnowledgeReference from(Object id) {
		if (id instanceof TinyflowKnowledgeReference) {
			return (TinyflowKnowledgeReference) id;
		}
		if (!(id instanceof Map)) {
			throw new IllegalArgumentException("knowledgeId must be a map with provider, tenantId and knowledgeId");
		}
		Map<?, ?> values = (Map<?, ?>) id;
		Object provider = values.containsKey("provider") ? values.get("provider") : values.get("providerKey");
		return builder()
				.provider(string(provider))
				.tenantId(string(values.get("tenantId")))
				.knowledgeId(string(values.get("knowledgeId")))
				.build();
	}

	public String getProvider() {
		return provider;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getKnowledgeId() {
		return knowledgeId;
	}

	private static String string(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	static String requireText(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value.trim();
	}

	public static final class Builder {
		private String provider;
		private String tenantId;
		private String knowledgeId;

		public Builder provider(String provider) {
			this.provider = provider;
			return this;
		}

		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		public Builder knowledgeId(String knowledgeId) {
			this.knowledgeId = knowledgeId;
			return this;
		}

		public TinyflowKnowledgeReference build() {
			return new TinyflowKnowledgeReference(this);
		}
	}
}
