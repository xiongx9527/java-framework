package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;

/**
 * AskData 对话生命周期中的厂商无关观测事件。
 *
 * <p>事件始终携带项目和操作人上下文，使发布端、日志存储端和外部观测适配器能够按
 * {@code projectId + operatorId} 隔离。{@code message} 只允许写入已经脱敏的摘要，禁止
 * 写入原始问题、SQL 参数、凭证或模型原始响应。</p>
 */
public class AskDataEvent {

	public enum ModelType {
		CHAT,
		EMBEDDING
	}

	public enum ModelState {
		ATTEMPTED,
		SUCCEEDED,
		FAILED
	}

	/**
	 * 对话执行过程必须覆盖的事件类型。
	 */
	public enum Type {
		/** 对话已创建或开始处理。 */
		CONVERSATION_STARTED,
		/** 问题已经完成计划。 */
		QUESTION_PLANNED,
		/** 问题规划失败。 */
		QUESTION_PLAN_FAILED,
		/** 子问题开始执行。 */
		SUB_QUESTION_STARTED,
		/** 子问题执行成功。 */
		SUB_QUESTION_COMPLETED,
		/** 子问题执行失败。 */
		SUB_QUESTION_FAILED,
		/** 子问题因依赖策略被跳过。 */
		SUB_QUESTION_SKIPPED,
		/** 子问题执行超时。 */
		SUB_QUESTION_TIMED_OUT,
		/** 子问题被取消。 */
		SUB_QUESTION_CANCELLED,
		/** 治理后的元数据已经解析。 */
		METADATA_RESOLVED,
		/** 已构建脱敏边界内的 Prompt。 */
		PROMPT_BUILT,
		/** 已选择模型或模型路由。 */
		MODEL_ROUTED,
		/** 已执行一个智能体节点。 */
		AGENT_NODE_EXECUTED,
		/** Agent 节点执行失败。 */
		AGENT_NODE_FAILED,
		/** 可选图运行时不可用，默认 Flow 回退到确定性顺序执行。 */
		GRAPH_RUNTIME_FALLBACK,
		/** 工具调用开始。 */
		TOOL_STARTED,
		/** 工具调用完成。 */
		TOOL_COMPLETED,
		/** 工具调用失败。 */
		TOOL_FAILED,
		/** SQL 插件阶段开始。 */
		SQL_PLUGIN_STARTED,
		/** SQL 插件阶段完成。 */
		SQL_PLUGIN_COMPLETED,
		/** SQL 插件阶段失败。 */
		SQL_PLUGIN_FAILED,
		/** SQL 插件、治理规则和参数已经完成组装。 */
		SQL_COMPOSED,
		/** SQL 组装失败。 */
		SQL_COMPOSITION_FAILED,
		/** SQL 已通过或未通过安全校验。 */
		SQL_VALIDATED,
		/** SQL 校验失败。 */
		SQL_VALIDATION_FAILED,
		/** SQL 即将进入受控执行。 */
		SQL_EXECUTION_STARTED,
		/** SQL 已提交执行并获得执行结果。 */
		SQL_EXECUTED,
		/** SQL 执行失败。 */
		SQL_EXECUTION_FAILED,
		/** 长期记忆召回开始。 */
		LONG_TERM_MEMORY_RECALL_STARTED,
		/** 长期记忆召回完成。 */
		LONG_TERM_MEMORY_RECALL_COMPLETED,
		/** 长期记忆召回失败。 */
		LONG_TERM_MEMORY_RECALL_FAILED,
		/** 上下文压缩开始。 */
		CONTEXT_COMPRESSION_STARTED,
		/** 上下文压缩已完成。 */
		CONTEXT_COMPRESSION_COMPLETED,
		/** 语义压缩失败并切换至确定性压缩。 */
		CONTEXT_COMPRESSION_DEGRADED,
		/** 上下文压缩完全失败。 */
		CONTEXT_COMPRESSION_FAILED,
		/** 检查点保存开始。 */
		CHECKPOINT_SAVE_STARTED,
		/** 可恢复的检查点已保存。 */
		CHECKPOINT_SAVED,
		/** 检查点保存失败。 */
		CHECKPOINT_SAVE_FAILED,
		/** 已尝试从检查点恢复。 */
		RECOVERY_ATTEMPTED,
		/** 已从检查点恢复。 */
		RECOVERY_COMPLETED,
		/** 检查点恢复失败。 */
		RECOVERY_FAILED,
		/** 外部监控存储失败并切换至本地诊断。 */
		MONITORING_DELIVERY_DEGRADED,
		/** 外部观测导出失败或任务被拒绝。 */
		OBSERVABILITY_EXPORT_DEGRADED,
		/** 对话已正常结束。 */
		CONVERSATION_COMPLETED,
		/** 对话已异常结束。 */
		CONVERSATION_FAILED,
		/** 对话被取消。 */
		CONVERSATION_CANCELLED,
		/** 治理授权拒绝或解析失败。 */
		GOVERNANCE_DENIED
	}

	private final AskDataProjectContext context;
	private final String traceId;
	private final Type type;
	private final String message;
	private final String datasourceKey;
	private final String denialCode;
	private final String authorizationVersion;
	private final String authorizationSource;
	private final ModelType modelType;
	private final String modelKey;
	private final AskDataModelRoutePolicy modelRoutePolicy;
	private final ModelState modelState;
	private final String modelFailureCode;
	private final String modelSwitchReason;
	private final String actualModelKey;
	private final AskDataEventCorrelation correlation;

	/**
	 * 创建一条已脱敏的生命周期事件。
	 *
	 * @param context 项目和操作人隔离上下文，不能为空
	 * @param traceId 本次对话或调用链的追踪标识，可由调用方按需关联
	 * @param type 生命周期事件类型，不能为空
	 * @param message 已脱敏的事件摘要，可为空
	 * @throws IllegalArgumentException 当上下文或事件类型为空时抛出
	 */
	public AskDataEvent(AskDataProjectContext context, String traceId, Type type, String message) {
		this(context, traceId, type, message, null, null, null, null, null, null, null, null, null, null, null,
				defaultCorrelation(traceId));
	}

	private AskDataEvent(AskDataProjectContext context, String traceId, Type type, String message,
			String datasourceKey, String denialCode, String authorizationVersion, String authorizationSource,
			ModelType modelType, String modelKey, AskDataModelRoutePolicy modelRoutePolicy, ModelState modelState,
			String modelFailureCode, String modelSwitchReason, String actualModelKey,
			AskDataEventCorrelation correlation) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		if (type == null) {
			throw new IllegalArgumentException("type 不能为空");
		}
		this.context = context;
		this.traceId = traceId;
		this.type = type;
		this.message = message;
		this.datasourceKey = datasourceKey;
		this.denialCode = denialCode;
		this.authorizationVersion = authorizationVersion;
		this.authorizationSource = authorizationSource;
		this.modelType = modelType;
		this.modelKey = modelKey;
		this.modelRoutePolicy = modelRoutePolicy;
		this.modelState = modelState;
		this.modelFailureCode = modelFailureCode;
		this.modelSwitchReason = modelSwitchReason;
		this.actualModelKey = actualModelKey;
		this.correlation = correlation;
	}

	/** 创建携带完整关联维度的已脱敏事件。 */
	public static AskDataEvent correlated(AskDataProjectContext context, String traceId, Type type, String message,
			AskDataEventCorrelation correlation) {
		if (correlation == null) {
			throw new IllegalArgumentException("correlation 不能为空");
		}
		return new AskDataEvent(context, traceId, type, message, null, null, null, null, null, null, null, null,
				null, null, null, correlation);
	}

	public static AskDataEvent governanceDenied(AskDataProjectContext context, String traceId, String datasourceKey,
			String denialCode, String authorizationVersion, String authorizationSource) {
		return new AskDataEvent(context, traceId, Type.GOVERNANCE_DENIED, "AskData 治理拒绝", datasourceKey,
				denialCode, authorizationVersion, authorizationSource, null, null, null, null, null, null, null,
				defaultCorrelation(traceId));
	}

	public static AskDataEvent modelRouted(AskDataProjectContext context, String traceId, String modelKey,
			String message) {
		if (modelKey == null || modelKey.trim().length() == 0) {
			throw new IllegalArgumentException("modelKey 不能为空");
		}
		return new AskDataEvent(context, traceId, Type.MODEL_ROUTED, message, null, null, null, null,
				ModelType.CHAT, modelKey.trim(), null, ModelState.ATTEMPTED, null, null, null,
				defaultCorrelation(traceId));
	}

	public static AskDataEvent modelAttempt(AskDataProjectContext context, String traceId, String modelKey,
			AskDataModelRoutePolicy routePolicy, ModelState state, String failureCode, String switchReason,
			String actualModelKey) {
		return modelAttempt(context, traceId, ModelType.CHAT, modelKey, routePolicy, state, failureCode,
				switchReason, actualModelKey);
	}

	public static AskDataEvent modelAttempt(AskDataProjectContext context, String traceId, ModelType modelType,
			String modelKey, AskDataModelRoutePolicy routePolicy, ModelState state, String failureCode,
			String switchReason, String actualModelKey) {
		return modelAttempt(context, traceId, modelType, modelKey, routePolicy, state, failureCode, switchReason,
				actualModelKey, defaultCorrelation(traceId));
	}

	/** 创建携带完整执行关联维度的模型尝试事件。 */
	public static AskDataEvent modelAttempt(AskDataProjectContext context, String traceId, ModelType modelType,
			String modelKey, AskDataModelRoutePolicy routePolicy, ModelState state, String failureCode,
			String switchReason, String actualModelKey, AskDataEventCorrelation correlation) {
		if (modelType == null || modelKey == null || modelKey.trim().length() == 0
				|| routePolicy == null || state == null) {
			throw new IllegalArgumentException("modelType、modelKey、routePolicy 和 state 不能为空");
		}
		if (state == ModelState.FAILED && (failureCode == null || failureCode.trim().length() == 0)) {
			throw new IllegalArgumentException("失败模型事件必须携带 failureCode");
		}
		return new AskDataEvent(context, traceId, Type.MODEL_ROUTED, "AskData 模型尝试", null, null, null, null,
				modelType, modelKey.trim(), routePolicy, state, normalize(failureCode), normalize(switchReason),
				normalize(actualModelKey), correlation);
	}

	/**
	 * 获取事件归属的项目和操作人上下文。
	 *
	 * @return 不为空的隔离上下文
	 */
	public AskDataProjectContext getContext() {
		return context;
	}

	/**
	 * 获取调用链追踪标识。
	 *
	 * @return 调用方提供的追踪标识，可为空
	 */
	public String getTraceId() {
		return traceId;
	}

	/**
	 * 获取生命周期事件类型。
	 *
	 * @return 不为空的事件类型
	 */
	public Type getType() {
		return type;
	}

	/**
	 * 获取已脱敏的事件摘要。
	 *
	 * @return 已脱敏摘要，可为空
	 */
	public String getMessage() {
		return message;
	}

	public String getDatasourceKey() { return datasourceKey; }
	public String getDenialCode() { return denialCode; }
	public String getAuthorizationVersion() { return authorizationVersion; }
	public String getAuthorizationSource() { return authorizationSource; }
	public ModelType getModelType() { return modelType; }
	public String getModelKey() { return modelKey; }
	public AskDataModelRoutePolicy getModelRoutePolicy() { return modelRoutePolicy; }
	public ModelState getModelState() { return modelState; }
	public String getModelFailureCode() { return modelFailureCode; }
	public String getModelSwitchReason() { return modelSwitchReason; }
	public String getActualModelKey() { return actualModelKey; }
	public AskDataEventCorrelation getCorrelation() { return correlation; }

	private static AskDataEventCorrelation defaultCorrelation(String traceId) {
		String normalized = normalize(traceId);
		return normalized == null ? null : AskDataEventCorrelation.builder(normalized, normalized).build();
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
