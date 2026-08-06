package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowKnowledgeResult {

	private final List<TinyflowKnowledgeMatch> matches;
	private final Map<String, Object> metadata;
	private final boolean knowledgeExists;

	private TinyflowKnowledgeResult(Builder builder) {
		this.matches = TinyflowKnowledgeRequest.immutableList(builder.matches);
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
		this.knowledgeExists = builder.knowledgeExists;
	}

	public static Builder builder() { return new Builder(); }
	public static TinyflowKnowledgeResult empty() { return builder().build(); }
	public static TinyflowKnowledgeResult notFound() { return builder().knowledgeExists(false).build(); }
	public List<TinyflowKnowledgeMatch> getMatches() { return matches; }
	public Map<String, Object> getMetadata() { return metadata; }
	public boolean isKnowledgeExists() { return knowledgeExists; }

	public static final class Builder {
		private List<TinyflowKnowledgeMatch> matches;
		private Map<String, Object> metadata;
		private boolean knowledgeExists = true;
		public Builder matches(List<TinyflowKnowledgeMatch> value) { this.matches = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public Builder knowledgeExists(boolean value) { this.knowledgeExists = value; return this; }
		public TinyflowKnowledgeResult build() { return new TinyflowKnowledgeResult(this); }
	}
}
