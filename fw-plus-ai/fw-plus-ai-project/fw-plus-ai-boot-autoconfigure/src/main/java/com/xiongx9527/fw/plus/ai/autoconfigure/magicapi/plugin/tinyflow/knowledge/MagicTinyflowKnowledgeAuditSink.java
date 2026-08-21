package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

public interface MagicTinyflowKnowledgeAuditSink {
	void record(AuditEvent event);

	final class AuditEvent {
		private final String tenantId;
		private final String action;
		private final String resourceId;
		private final boolean success;
		private final String errorType;

		public AuditEvent(String tenantId, String action, String resourceId, boolean success, String errorType) {
			this.tenantId = tenantId;
			this.action = action;
			this.resourceId = resourceId;
			this.success = success;
			this.errorType = errorType;
		}
		public String getTenantId() { return tenantId; }
		public String getAction() { return action; }
		public String getResourceId() { return resourceId; }
		public boolean isSuccess() { return success; }
		public String getErrorType() { return errorType; }
		@Override public String toString() {
			return "AuditEvent{tenantId='" + tenantId + "', action='" + action + "', resourceId='"
					+ resourceId + "', success=" + success + ", errorType='" + errorType + "'}";
		}
	}
}
