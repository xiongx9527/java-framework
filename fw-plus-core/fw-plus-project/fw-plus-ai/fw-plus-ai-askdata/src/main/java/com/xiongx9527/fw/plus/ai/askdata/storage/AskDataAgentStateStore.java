package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData Agent 状态存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离状态。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataAgentStateStore {

	void save(AskDataProjectContext context, String agentId, AgentState state);

	AgentState load(AskDataProjectContext context, String agentId);

	/**
	 * Agent 的最小运行状态。
	 */
	class AgentState {

		private final String status;
		private final String stage;

		public AgentState(String status, String stage) {
			this.status = status;
			this.stage = stage;
		}

		public String getStatus() {
			return status;
		}

		public String getStage() {
			return stage;
		}
	}
}
