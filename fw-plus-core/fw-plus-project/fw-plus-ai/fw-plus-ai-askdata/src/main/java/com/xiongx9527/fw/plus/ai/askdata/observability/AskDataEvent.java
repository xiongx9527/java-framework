package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 对话生命周期中的厂商无关观测事件。
 *
 * <p>事件始终携带项目和操作人上下文，使发布端、日志存储端和外部观测适配器能够按
 * {@code projectId + operatorId} 隔离。{@code message} 只允许写入已经脱敏的摘要，禁止
 * 写入原始问题、SQL 参数、凭证或模型原始响应。</p>
 */
public class AskDataEvent {

	/**
	 * 对话执行过程必须覆盖的事件类型。
	 */
	public enum Type {
		/** 对话已创建或开始处理。 */
		CONVERSATION_STARTED,
		/** 问题已经完成计划。 */
		QUESTION_PLANNED,
		/** 已选择模型或模型路由。 */
		MODEL_ROUTED,
		/** 已执行一个智能体节点。 */
		AGENT_NODE_EXECUTED,
		/** SQL 已通过或未通过安全校验。 */
		SQL_VALIDATED,
		/** SQL 已提交执行并获得执行结果。 */
		SQL_EXECUTED,
		/** 可恢复的检查点已保存。 */
		CHECKPOINT_SAVED,
		/** 已尝试从检查点恢复。 */
		RECOVERY_ATTEMPTED,
		/** 对话已正常结束。 */
		CONVERSATION_COMPLETED,
		/** 对话已异常结束。 */
		CONVERSATION_FAILED
	}

	private final AskDataProjectContext context;
	private final String traceId;
	private final Type type;
	private final String message;

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
}
