package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

/**
 * 工具调用参数错误处理器：「必填参数缺失」与「参数绑定失败」如何回文本给 LLM，
 * 策略由业务来定——缺参时通常提示反问用户补齐，格式错误时通常提示 LLM 修正后重试。
 *
 * <p>langchain4j 0.35 没有 {@code AiServices.toolArgumentsErrorHandler(...)} 扩展点：
 * 缺参会静默传 {@code null} 继续执行，参数绑定失败会直接抛给业务调用方。
 * 本接口配合 {@link GuardedToolExecutor} 在 {@code tools(...)} 与 {@code toolProvider(...)}
 * 两条注册路径上补齐等价能力；返回的文本会作为 {@code ToolExecutionResultMessage}
 * 回到 LLM 的下一轮上下文。升级 langchain4j 1.x 后可改为原生扩展点透传。</p>
 *
 * <p>处理器也可以选择直接抛出异常（如决定中断本轮调用）：抛出的异常不会被吞掉，
 * 会继续向业务调用方传播。</p>
 */
public interface ToolArgumentsErrorHandler {

	/**
	 * 处理一次工具参数错误。
	 *
	 * @param error   缺参时为 {@link MissingArgumentException}；绑定/格式错误时为
	 *                {@link IllegalArgumentException} 等
	 * @param request 原始工具调用请求（含工具名与 arguments JSON）
	 * @return 回给 LLM 的工具结果文本
	 */
	String handle(Throwable error, ToolExecutionRequest request);

	/**
	 * 内置默认策略（业务未通过 {@code AssistantBuilder.toolArgumentsErrorHandler(...)} 配置时生效）：
	 * 缺参 → 提示 LLM 向用户询问缺失信息；其他参数错误 → 提示 LLM 修正后重试。
	 */
	ToolArgumentsErrorHandler DEFAULT = (error, request) -> {
		if (error instanceof MissingArgumentException) {
			MissingArgumentException missing = (MissingArgumentException) error;
			return "工具调用缺少必要参数，请向用户询问缺失的信息，等用户补齐参数后再调用。工具："
					+ missing.getToolName() + "，缺失参数：" + missing.getMissingParameters();
		}
		return "工具调用参数格式错误，请修正参数后重试。工具：" + request.name()
				+ "，原因：" + error.getMessage();
	};

}
