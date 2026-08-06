package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;
import java.util.Map;

public interface TinyflowChunkStrategy {

	List<TinyflowChunk> chunk(TinyflowParsedDocument parsed, ChunkOptions options);

	final class ChunkOptions {
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String sourceType;
		private final String documentId;
		private final Map<String, Object> options;

		public ChunkOptions(String tenantId, String knowledgeId, String sourceId, String sourceType,
				String documentId, Map<String, Object> options) {
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.sourceId = TinyflowKnowledgeReference.requireText(sourceId, "sourceId");
			this.sourceType = TinyflowKnowledgeReference.requireText(sourceType, "sourceType");
			this.documentId = TinyflowKnowledgeReference.requireText(documentId, "documentId");
			this.options = TinyflowKnowledgeRequest.immutableMap(options);
		}

		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getSourceType() { return sourceType; }
		public String getDocumentId() { return documentId; }
		public Map<String, Object> getOptions() { return options; }
	}
}
