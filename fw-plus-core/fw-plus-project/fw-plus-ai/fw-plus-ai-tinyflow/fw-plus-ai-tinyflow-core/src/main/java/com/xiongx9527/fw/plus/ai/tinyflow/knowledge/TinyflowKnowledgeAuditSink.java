package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public interface TinyflowKnowledgeAuditSink {

	void record(AuditEvent event);

	final class AuditEvent {
		private final String eventId;
		private final String tenantId;
		private final String knowledgeId;
		private final String resourceType;
		private final String resourceId;
		private final String action;
		private final String outcome;
		private final String actorId;
		private final long occurredAtEpochMillis;

		public AuditEvent(String eventId, String tenantId, String knowledgeId, String resourceType,
				String resourceId, String action, String outcome, String actorId, long occurredAtEpochMillis) {
			this.eventId = TinyflowKnowledgeReference.requireText(eventId, "eventId");
			this.tenantId = TinyflowKnowledgeReference.requireText(tenantId, "tenantId");
			this.knowledgeId = TinyflowKnowledgeReference.requireText(knowledgeId, "knowledgeId");
			this.resourceType = TinyflowKnowledgeReference.requireText(resourceType, "resourceType");
			this.resourceId = TinyflowKnowledgeReference.requireText(resourceId, "resourceId");
			this.action = TinyflowKnowledgeReference.requireText(action, "action");
			this.outcome = TinyflowKnowledgeReference.requireText(outcome, "outcome");
			this.actorId = TinyflowKnowledgeReference.requireText(actorId, "actorId");
			this.occurredAtEpochMillis = occurredAtEpochMillis;
		}

		public String getEventId() { return eventId; }
		public String getTenantId() { return tenantId; }
		public String getKnowledgeId() { return knowledgeId; }
		public String getResourceType() { return resourceType; }
		public String getResourceId() { return resourceId; }
		public String getAction() { return action; }
		public String getOutcome() { return outcome; }
		public String getActorId() { return actorId; }
		public long getOccurredAtEpochMillis() { return occurredAtEpochMillis; }
	}
}
