package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowDocument {

	private final String tenantId;
	private final String knowledgeId;
	private final String sourceId;
	private final String sourceType;
	private final String documentId;
	private final String name;
	private final String content;
	private final String sourceUri;
	private final String sourceVersion;
	private final Map<String, Object> metadata;
	private final List<TinyflowChunk> chunks;

	private TinyflowDocument(Builder builder) {
		this.tenantId = TinyflowChunk.required(builder.tenantId, "tenantId");
		this.knowledgeId = TinyflowChunk.required(builder.knowledgeId, "knowledgeId");
		this.sourceId = TinyflowChunk.required(builder.sourceId, "sourceId");
		this.sourceType = TinyflowChunk.required(builder.sourceType, "sourceType");
		this.documentId = TinyflowChunk.required(builder.documentId, "documentId");
		this.name = builder.name;
		this.content = builder.content;
		this.sourceUri = builder.sourceUri;
		this.sourceVersion = builder.sourceVersion;
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
		this.chunks = TinyflowKnowledgeRequest.immutableList(builder.chunks);
	}

	public static Builder builder() { return new Builder(); }
	public String getTenantId() { return tenantId; }
	public String getKnowledgeId() { return knowledgeId; }
	public String getSourceId() { return sourceId; }
	public String getSourceType() { return sourceType; }
	public String getDocumentId() { return documentId; }
	public String getName() { return name; }
	public String getContent() { return content; }
	public String getSourceUri() { return sourceUri; }
	public String getSourceVersion() { return sourceVersion; }
	public Map<String, Object> getMetadata() { return metadata; }
	public List<TinyflowChunk> getChunks() { return chunks; }

	public static final class Builder {
		private String tenantId;
		private String knowledgeId;
		private String sourceId;
		private String sourceType;
		private String documentId;
		private String name;
		private String content;
		private String sourceUri;
		private String sourceVersion;
		private Map<String, Object> metadata;
		private List<TinyflowChunk> chunks;
		public Builder tenantId(String value) { this.tenantId = value; return this; }
		public Builder knowledgeId(String value) { this.knowledgeId = value; return this; }
		public Builder sourceId(String value) { this.sourceId = value; return this; }
		public Builder sourceType(String value) { this.sourceType = value; return this; }
		public Builder documentId(String value) { this.documentId = value; return this; }
		public Builder name(String value) { this.name = value; return this; }
		public Builder content(String value) { this.content = value; return this; }
		public Builder sourceUri(String value) { this.sourceUri = value; return this; }
		public Builder sourceVersion(String value) { this.sourceVersion = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public Builder chunks(List<TinyflowChunk> value) { this.chunks = value; return this; }
		public TinyflowDocument build() { return new TinyflowDocument(this); }
	}
}
