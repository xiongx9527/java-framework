package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Map;

public interface TinyflowKnowledgeAuditSink {

	void record(AuditEvent event);

	final class AuditEvent {
		private final String eventId;
		private final String tenantId;
		private final String knowledgeId;
		private final String sourceId;
		private final String documentId;
		private final String action;
		private final long occurredAtEpochMillis;
		private final Map<String, Object> metadata;

		public AuditEvent(String eventId, String tenantId, String knowledgeId, String sourceId,
				String documentId, String action, long occurredAtEpochMillis, Map<String, Object> metadata) {
			this.eventId = TinyflowKnowledgeReference.requireText(eventId, "eventId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.sourceId = sourceId;
			this.documentId = documentId;
			this.action = TinyflowKnowledgeReference.requireText(action, "action");
			this.occurredAtEpochMillis = occurredAtEpochMillis;
			this.metadata = TinyflowKnowledgeRequest.immutableMap(metadata);
		}

		public String getEventId() { return eventId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getSourceId() { return sourceId; }
		public String getDocumentId() { return documentId; }
		public String getAction() { return action; }
		public long getOccurredAtEpochMillis() { return occurredAtEpochMillis; }
		public Map<String, Object> getMetadata() { return metadata; }
	}
}
