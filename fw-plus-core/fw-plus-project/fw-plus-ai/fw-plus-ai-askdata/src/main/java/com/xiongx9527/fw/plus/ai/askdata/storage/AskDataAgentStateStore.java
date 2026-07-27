package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData Agent 状态存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离状态。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataAgentStateStore {

	/**
	 * 保存 Agent 运行状态。
	 *
	 * <p>实现必须以 {@code projectId + operatorId + agentId} 隔离状态，禁止不同项目或操作人之间共享同一 Agent
	 * 标识的数据。默认内存实现仅保存于当前单机进程，集群必须使用共享存储实现以保持状态一致。</p>
	 */
	void save(AskDataProjectContext context, String agentId, AgentState state);

	/**
	 * 按当前项目和操作人读取 Agent 运行状态。
	 *
	 * <p>只返回 {@code projectId + operatorId + agentId} 对应的状态；不存在时返回 {@code null}，不得跨项目或跨操作人
	 * 回退查询。默认内存实现不提供跨节点共享能力。</p>
	 */
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
