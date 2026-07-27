package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 检查点存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离检查点。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataCheckpointStore {

	/**
	 * 保存检查点并返回已保存值。
	 *
	 * <p>实现必须按 {@code projectId + operatorId + checkpointId} 隔离检查点。对于同一隔离范围和检查点标识，
	 * 新检查点版本必须严格大于已有版本；相同或更小版本必须拒绝，避免恢复状态回退。</p>
	 *
	 * <p>默认内存实现仅适合单机进程。集群环境必须提供共享存储实现，以保证各节点读取到同一版本序列。</p>
	 */
	Checkpoint save(AskDataProjectContext context, String checkpointId, Checkpoint checkpoint);

	/**
	 * 按当前项目和操作人读取检查点。
	 *
	 * <p>只允许读取 {@code projectId + operatorId + checkpointId} 对应的数据，不存在时返回 {@code null}，不得跨项目
	 * 或跨操作人读取。读取到 {@link SqlExecutionState#UNKNOWN} 的检查点时，调用方不得将 SQL 标记为可重放。</p>
	 *
	 * <p>默认内存实现的数据不跨进程共享；集群部署应替换为共享存储实现。</p>
	 */
	Checkpoint load(AskDataProjectContext context, String checkpointId);

	/**
	 * SQL 执行状态。UNKNOWN 表示执行结果不确定，不能据此重放 SQL。
	 */
	enum SqlExecutionState {
		NOT_EXECUTED, SUCCEEDED, FAILED, UNKNOWN
	}

	/**
	 * 带版本的检查点值对象。
	 */
	class Checkpoint {

		private final long version;
		private final String stage;
		private final SqlExecutionState sqlExecutionState;

		public Checkpoint(long version, String stage, SqlExecutionState sqlExecutionState) {
			if (version < 1) {
				throw new IllegalArgumentException("checkpoint version 必须大于 0");
			}
			if (sqlExecutionState == null) {
				throw new IllegalArgumentException("sqlExecutionState 不能为空");
			}
			this.version = version;
			this.stage = stage;
			this.sqlExecutionState = sqlExecutionState;
		}

		public long getVersion() {
			return version;
		}

		public String getStage() {
			return stage;
		}

		public SqlExecutionState getSqlExecutionState() {
			return sqlExecutionState;
		}

		public boolean isVersionAdvancedFrom(Checkpoint previous) {
			return previous != null && version > previous.version;
		}

		public boolean isReplayable() {
			return sqlExecutionState == SqlExecutionState.NOT_EXECUTED;
		}
	}
}
