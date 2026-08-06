package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowKnowledgeResult {

	private final List<TinyflowKnowledgeMatch> matches;
	private final Map<String, Object> metadata;

	private TinyflowKnowledgeResult(Builder builder) {
		this.matches = TinyflowKnowledgeRequest.immutableList(builder.matches);
		this.metadata = TinyflowKnowledgeRequest.immutableMap(builder.metadata);
	}

	public static Builder builder() { return new Builder(); }
	public static TinyflowKnowledgeResult empty() { return builder().build(); }
	public List<TinyflowKnowledgeMatch> getMatches() { return matches; }
	public Map<String, Object> getMetadata() { return metadata; }

	public static final class Builder {
		private List<TinyflowKnowledgeMatch> matches;
		private Map<String, Object> metadata;
		public Builder matches(List<TinyflowKnowledgeMatch> value) { this.matches = value; return this; }
		public Builder metadata(Map<String, Object> value) { this.metadata = value; return this; }
		public TinyflowKnowledgeResult build() { return new TinyflowKnowledgeResult(this); }
	}
}
