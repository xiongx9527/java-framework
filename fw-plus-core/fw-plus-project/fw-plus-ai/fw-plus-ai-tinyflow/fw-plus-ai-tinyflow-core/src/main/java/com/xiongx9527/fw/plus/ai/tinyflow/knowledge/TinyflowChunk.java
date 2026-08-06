package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowChunk {

	private final String sourceId;
	private final String sourceType;
	private final String documentId;
	private final String chunkId;
	private final String content;
	private final String sourceUri;
	private final String sourceVersion;
	private final String location;
	private final Integer position;
	private final String parentChunkId;
	private final Integer tokenCount;
	private final Map<String, Object> metadata;
	private final boolean enabled;
	private final List<String> vectorIds;

	private TinyflowChunk(Builder builder) {
		this.sourceId = required(builder.sourceId, "sourceId");
		this.sourceType = required(builder.sourceType, "sourceType");
		this.documentId = required(builder.documentId, "documentId");
		this.chunkId = required(builder.chunkId, "chunkId");
		this.content = required(builder.content, "content");
		this.sourceUri = builder.sourceUri;
		this.sourceVersion = builder.sourceVersion;
		this.location = builder.location;
		this.position = builder.position;
		this.parentChunkId = builder.parentChunkId;
		this.tokenCount = builder.tokenCount;
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
		this.enabled = builder.enabled;
		this.vectorIds = TinyflowKnowledgeRequest.immutableList(builder.vectorIds);
	}

	public static Builder builder() { return new Builder(); }
	static String required(String value, String name) { return TinyflowKnowledgeReference.requireText(value, name); }

	public String getSourceId() { return sourceId; }
	public String getSourceType() { return sourceType; }
	public String getDocumentId() { return documentId; }
	public String getChunkId() { return chunkId; }
	public String getContent() { return content; }
	public String getSourceUri() { return sourceUri; }
	public String getSourceVersion() { return sourceVersion; }
	public String getLocation() { return location; }
	public Integer getPosition() { return position; }
	public String getParentChunkId() { return parentChunkId; }
	public Integer getTokenCount() { return tokenCount; }
	public Map<String, Object> getMetadata() { return metadata; }
	public boolean isEnabled() { return enabled; }
	public List<String> getVectorIds() { return vectorIds; }

	public static final class Builder {
		private String sourceId;
		private String sourceType;
		private String documentId;
		private String chunkId;
		private String content;
		private String sourceUri;
		private String sourceVersion;
		private String location;
		private Integer position;
		private String parentChunkId;
		private Integer tokenCount;
		private Map<String, Object> metadata;
		private boolean enabled = true;
		private List<String> vectorIds;

		public Builder sourceId(String value) { this.sourceId = value; return this; }
		public Builder sourceType(String value) { this.sourceType = value; return this; }
		public Builder documentId(String value) { this.documentId = value; return this; }
		public Builder chunkId(String value) { this.chunkId = value; return this; }
		public Builder content(String value) { this.content = value; return this; }
		public Builder sourceUri(String value) { this.sourceUri = value; return this; }
		public Builder sourceVersion(String value) { this.sourceVersion = value; return this; }
		public Builder location(String value) { this.location = value; return this; }
		public Builder position(Integer value) { this.position = value; return this; }
		public Builder parentChunkId(String value) { this.parentChunkId = value; return this; }
		public Builder tokenCount(Integer value) { this.tokenCount = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public Builder enabled(boolean value) { this.enabled = value; return this; }
		public Builder vectorIds(List<String> value) { this.vectorIds = value; return this; }
		public TinyflowChunk build() { return new TinyflowChunk(this); }
	}
}
