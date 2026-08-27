package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public final class TinyflowIngestionTask {

	private final String taskId;
	private final String providerKey;
	private final String tenantId;
	private final String knowledgeId;
	private final String documentId;
	private final String status;
	private final String message;
	private final Map<String, Object> metadata;

	private TinyflowIngestionTask(Builder builder) {
		this.taskId = TinyflowKnowledgeReference.requireText(builder.taskId, "taskId");
		this.providerKey = TinyflowKnowledgeReference.requireText(builder.providerKey, "providerKey");
		this.tenantId = TinyflowKnowledgeReference.requireText(builder.tenantId, "tenantId");
		this.knowledgeId = TinyflowKnowledgeReference.requireText(builder.knowledgeId, "knowledgeId");
		this.documentId = TinyflowKnowledgeReference.requireText(builder.documentId, "documentId");
		this.status = TinyflowKnowledgeReference.requireText(builder.status, "status");
		this.message = builder.message;
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
	}

	public static Builder builder() { return new Builder(); }
	public String getTaskId() { return taskId; }
	public String getProviderKey() { return providerKey; }
	public String getTenantId() { return tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public String getDocumentId() { return documentId; }
	public String getStatus() { return status; }
	public String getMessage() { return message; }
	public Map<String, Object> getMetadata() { return metadata; }

	public static final class Builder {
		private String taskId;
		private String providerKey;
		private String tenantId;
		private String knowledgeId;
		private String documentId;
		private String status;
		private String message;
		private Map<String, Object> metadata;
		public Builder taskId(String value) { this.taskId = value; return this; }
		public Builder providerKey(String value) { this.providerKey = value; return this; }
		public Builder tenantId(String value) { this.tenantId = value; return this; }
		public Builder knowledgeId(String value) { this.knowledgeId = value; return this; }
		public Builder documentId(String value) { this.documentId = value; return this; }
		public Builder status(String value) { this.status = value; return this; }
		public Builder message(String value) { this.message = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public TinyflowIngestionTask build() { return new TinyflowIngestionTask(this); }
	}
}
