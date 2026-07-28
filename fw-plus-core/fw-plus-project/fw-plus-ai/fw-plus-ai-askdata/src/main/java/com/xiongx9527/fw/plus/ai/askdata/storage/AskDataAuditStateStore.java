package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 以项目和操作人隔离的审计状态 SPI。 */
public interface AskDataAuditStateStore {

	void save(AskDataProjectContext context, String auditId, AuditState state);

	AuditState load(AskDataProjectContext context, String auditId);

	class AuditState {
		private final String status;
		private final String detail;
		private final long sequence;
		public AuditState(String status, String detail, long sequence) { this.status = status; this.detail = detail; this.sequence = sequence; }
		public String getStatus() { return status; }
		public String getDetail() { return detail; }
		public long getSequence() { return sequence; }
	}
}
