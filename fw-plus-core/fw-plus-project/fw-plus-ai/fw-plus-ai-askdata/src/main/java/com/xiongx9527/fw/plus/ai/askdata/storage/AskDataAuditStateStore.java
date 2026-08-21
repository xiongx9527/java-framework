package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 以项目和操作人隔离的审计终态 SPI。
 *
 * <p>实现必须保证同一审计 ID 的 sequence 严格推进，拒绝乱序覆盖；集群部署应使用事务或原子比较更新。
 * 审计状态只保存脱敏终态和错误分类，不保存凭据、原始模型响应、完整 SQL 参数或结果行。业务可以延长保留期，
 * 但不能通过删除审计来改变已经完成的执行结果。</p>
 */
public interface AskDataAuditStateStore {

	void save(AskDataProjectContext context, String auditId, AuditState state);

	AuditState load(AskDataProjectContext context, String auditId);

	/** 不可变的审计终态。 */
	class AuditState {
		private final String status;
		private final String detail;
		private final long sequence;

		public AuditState(String status, String detail, long sequence) {
			this.status = status;
			this.detail = detail;
			this.sequence = sequence;
		}

		public String getStatus() { return status; }
		public String getDetail() { return detail; }
		public long getSequence() { return sequence; }
	}
}
