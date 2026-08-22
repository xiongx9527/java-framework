package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public final class TinyflowParsedDocument {

	private final String title;
	private final String content;
	private final List<Section> sections;
	private final Map<String, Object> metadata;

	public TinyflowParsedDocument(String title, String content, List<Section> sections,
			Map<String, Object> metadata) {
		this.title = title;
		this.content = TinyflowKnowledgeReference.requireText(content, "content");
		this.sections = TinyflowKnowledgeRequest.immutableList(sections);
		this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
	}

	public String getTitle() { return title; }
	public String getContent() { return content; }
	public List<Section> getSections() { return sections; }
	public Map<String, Object> getMetadata() { return metadata; }

	public static final class Section {
		private final String content;
		private final String location;
		private final Map<String, Object> metadata;

		public Section(String content, String location, Map<String, Object> metadata) {
			this.content = TinyflowKnowledgeReference.requireText(content, "content");
			this.location = location;
			this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
		}

		public String getContent() { return content; }
		public String getLocation() { return location; }
		public Map<String, Object> getMetadata() { return metadata; }
	}
}
