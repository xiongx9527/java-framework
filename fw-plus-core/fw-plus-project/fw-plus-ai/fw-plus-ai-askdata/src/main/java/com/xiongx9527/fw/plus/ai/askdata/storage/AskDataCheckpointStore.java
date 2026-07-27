package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 检查点存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离检查点。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataCheckpointStore {

	Checkpoint save(AskDataProjectContext context, String checkpointId, Checkpoint checkpoint);

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
