package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		private final String compressedContext;
		private final ContextCompressionStatus contextCompressionStatus;
		private final List<NodeState> nodes;
		private final List<SubTaskState> subTasks;
		private final List<String> plan;
		private final List<ToolSummary> toolSummaries;
		private final List<ModelRouteAttempt> modelRouteAttempts;
		private final List<StepLog> stepLogs;

		public AgentState(String status, String stage) {
			this(status, stage, null, ContextCompressionStatus.NOT_COMPRESSED, Collections.<NodeState>emptyList(),
					Collections.<SubTaskState>emptyList(), Collections.<String>emptyList(),
					Collections.<ToolSummary>emptyList(), Collections.<ModelRouteAttempt>emptyList(),
					Collections.<StepLog>emptyList());
		}

		public AgentState(String status, String stage, String compressedContext,
				ContextCompressionStatus contextCompressionStatus, List<NodeState> nodes, List<SubTaskState> subTasks,
				List<String> plan, List<ToolSummary> toolSummaries, List<ModelRouteAttempt> modelRouteAttempts,
				List<StepLog> stepLogs) {
			this.status = status;
			this.stage = stage;
			this.compressedContext = compressedContext;
			this.contextCompressionStatus = contextCompressionStatus == null ? ContextCompressionStatus.NOT_COMPRESSED
					: contextCompressionStatus;
			this.nodes = immutableCopy(nodes);
			this.subTasks = immutableCopy(subTasks);
			this.plan = immutableCopy(plan);
			this.toolSummaries = immutableCopy(toolSummaries);
			this.modelRouteAttempts = immutableCopy(modelRouteAttempts);
			this.stepLogs = immutableCopy(stepLogs);
		}

		public String getStatus() {
			return status;
		}

		public String getStage() {
			return stage;
		}

		public String getCompressedContext() { return compressedContext; }
		public ContextCompressionStatus getContextCompressionStatus() { return contextCompressionStatus; }
		public List<NodeState> getNodes() { return nodes; }
		public List<SubTaskState> getSubTasks() { return subTasks; }
		public List<String> getPlan() { return plan; }
		public List<ToolSummary> getToolSummaries() { return toolSummaries; }
		public List<ModelRouteAttempt> getModelRouteAttempts() { return modelRouteAttempts; }
		public List<StepLog> getStepLogs() { return stepLogs; }

		private static <T> List<T> immutableCopy(List<T> values) {
			return values == null ? Collections.<T>emptyList() : Collections.unmodifiableList(new ArrayList<T>(values));
		}
	}

	enum ContextCompressionStatus { NOT_COMPRESSED, COMPRESSING, COMPRESSED, FAILED }

	class NodeState {
		private final String nodeId; private final String status;
		public NodeState(String nodeId, String status) { this.nodeId = nodeId; this.status = status; }
		public String getNodeId() { return nodeId; } public String getStatus() { return status; }
	}
	class SubTaskState {
		private final String taskId; private final String name; private final String status;
		private final String failurePolicy; private final String errorCode;
		public SubTaskState(String taskId, String name, String status) { this(taskId, name, status, null, null); }
		public SubTaskState(String taskId, String name, String status, String failurePolicy, String errorCode) {
			this.taskId = taskId; this.name = name; this.status = status;
			this.failurePolicy = failurePolicy; this.errorCode = errorCode;
		}
		public String getTaskId() { return taskId; } public String getName() { return name; }
		public String getStatus() { return status; } public String getFailurePolicy() { return failurePolicy; }
		public String getErrorCode() { return errorCode; }
	}
	class ToolSummary {
		private final String toolName; private final String status; private final String summary;
		public ToolSummary(String toolName, String status, String summary) { this.toolName = toolName; this.status = status; this.summary = summary; }
		public String getToolName() { return toolName; } public String getStatus() { return status; } public String getSummary() { return summary; }
	}
	class ModelRouteAttempt {
		private final String modelKey; private final String status; private final String detail;
		private final String modelType; private final String routePolicy; private final String routeReason;
		private final String failureCode; private final String switchReason; private final String actualModelKey;
		public ModelRouteAttempt(String modelKey, String status, String detail) {
			this(modelKey, status, detail, null, null, null, null, null, null);
		}
		public ModelRouteAttempt(String modelKey, String status, String detail, String modelType,
				String routePolicy, String routeReason, String failureCode, String switchReason,
				String actualModelKey) {
			this.modelKey = modelKey; this.status = status; this.detail = detail; this.modelType = modelType;
			this.routePolicy = routePolicy; this.routeReason = routeReason; this.failureCode = failureCode;
			this.switchReason = switchReason; this.actualModelKey = actualModelKey;
		}
		public String getModelKey() { return modelKey; } public String getStatus() { return status; }
		public String getDetail() { return detail; } public String getModelType() { return modelType; }
		public String getRoutePolicy() { return routePolicy; } public String getRouteReason() { return routeReason; }
		public String getFailureCode() { return failureCode; } public String getSwitchReason() { return switchReason; }
		public String getActualModelKey() { return actualModelKey; }
	}
	class StepLog {
		private final String step; private final String status; private final String detail;
		public StepLog(String step, String status, String detail) { this.step = step; this.status = status; this.detail = detail; }
		public String getStep() { return step; } public String getStatus() { return status; } public String getDetail() { return detail; }
	}
}
