package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowKnowledgeMatch {

	private final TinyflowChunk chunk;
	private final double score;

	private TinyflowKnowledgeMatch(Builder builder) {
		this.chunk = TinyflowChunk.builder().sourceId(builder.sourceId).sourceType(builder.sourceType)
				.documentId(builder.documentId).chunkId(builder.chunkId).content(builder.content)
				.sourceUri(builder.sourceUri).sourceVersion(builder.sourceVersion).location(builder.location)
				.position(builder.position).parentChunkId(builder.parentChunkId).tokenCount(builder.tokenCount)
				.metadata(builder.metadata).enabled(builder.enabled).vectorIds(builder.vectorIds).build();
		if (Double.isNaN(builder.score) || Double.isInfinite(builder.score)) {
			throw new IllegalArgumentException("score must be finite");
		}
		this.score = builder.score;
	}

	public static Builder builder() { return new Builder(); }
	public String getSourceId() { return chunk.getSourceId(); }
	public String getSourceType() { return chunk.getSourceType(); }
	public String getDocumentId() { return chunk.getDocumentId(); }
	public String getChunkId() { return chunk.getChunkId(); }
	public String getContent() { return chunk.getContent(); }
	public String getSourceUri() { return chunk.getSourceUri(); }
	public String getSourceVersion() { return chunk.getSourceVersion(); }
	public String getLocation() { return chunk.getLocation(); }
	public Integer getPosition() { return chunk.getPosition(); }
	public String getParentChunkId() { return chunk.getParentChunkId(); }
	public Integer getTokenCount() { return chunk.getTokenCount(); }
	public Map<String, Object> getMetadata() { return chunk.getMetadata(); }
	public boolean isEnabled() { return chunk.isEnabled(); }
	public List<String> getVectorIds() { return chunk.getVectorIds(); }
	public double getScore() { return score; }
	public TinyflowChunk getChunk() { return chunk; }

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
		private double score;

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
		public Builder score(double value) { this.score = value; return this; }
		public TinyflowKnowledgeMatch build() { return new TinyflowKnowledgeMatch(this); }
	}
}
