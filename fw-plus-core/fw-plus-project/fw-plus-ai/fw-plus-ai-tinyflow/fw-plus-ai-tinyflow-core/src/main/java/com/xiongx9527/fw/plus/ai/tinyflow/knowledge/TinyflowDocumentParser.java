package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface TinyflowDocumentParser {

	boolean supports(String mediaType, String fileName);

	TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException;

	final class ParseContext {
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String sourceType;
		private final String documentId;
		private final String mediaType;
		private final String fileName;
		private final Map<String, Object> options;

		public ParseContext(String tenantId, String knowledgeId, String sourceId, String sourceType,
				String documentId, String mediaType, String fileName, Map<String, Object> options) {
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.sourceId = TinyflowKnowledgeReference.requireText(sourceId, "sourceId");
			this.sourceType = TinyflowKnowledgeReference.requireText(sourceType, "sourceType");
			this.documentId = TinyflowKnowledgeReference.requireText(documentId, "documentId");
			this.mediaType = mediaType;
			this.fileName = fileName;
			this.options = TinyflowKnowledgeRequest.immutableMap(options);
		}

		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getSourceType() { return sourceType; }
		public String getDocumentId() { return documentId; }
		public String getMediaType() { return mediaType; }
		public String getFileName() { return fileName; }
		public Map<String, Object> getOptions() { return options; }
	}
}
