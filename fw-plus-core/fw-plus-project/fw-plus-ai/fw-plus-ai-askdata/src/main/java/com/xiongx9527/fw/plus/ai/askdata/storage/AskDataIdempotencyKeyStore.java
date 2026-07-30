package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 以项目和操作人隔离的请求幂等键 SPI。 */
public interface AskDataIdempotencyKeyStore {

	boolean recordIfAbsent(AskDataProjectContext context, String idempotencyKey, IdempotencyRecord record);

	IdempotencyRecord load(AskDataProjectContext context, String idempotencyKey);

	class IdempotencyRecord {
		private final String status;
		private final String referenceId;
		public IdempotencyRecord(String status, String referenceId) { this.status = status; this.referenceId = referenceId; }
		public String getStatus() { return status; }
		public String getReferenceId() { return referenceId; }
	}
}
