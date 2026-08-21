package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 以项目和操作人隔离的请求幂等键 SPI。
 *
 * <p>{@link #recordIfAbsent} 必须是原子写入；集群实现应使用唯一键、SET NX 或等价能力。相同幂等键只能
 * 关联同一会话或子任务，禁止跨项目、跨操作人去重，也不能在竞争失败后继续执行副作用步骤。记录内容应为
 * 脱敏状态和稳定引用，不得保存原始问题、SQL 或凭据。</p>
 */
public interface AskDataIdempotencyKeyStore {

	boolean recordIfAbsent(AskDataProjectContext context, String idempotencyKey, IdempotencyRecord record);

	IdempotencyRecord load(AskDataProjectContext context, String idempotencyKey);

	/** 不可变的幂等状态和执行引用。 */
	class IdempotencyRecord {
		private final String status;
		private final String referenceId;

		public IdempotencyRecord(String status, String referenceId) {
			this.status = status;
			this.referenceId = referenceId;
		}

		public String getStatus() { return status; }
		public String getReferenceId() { return referenceId; }
	}
}
