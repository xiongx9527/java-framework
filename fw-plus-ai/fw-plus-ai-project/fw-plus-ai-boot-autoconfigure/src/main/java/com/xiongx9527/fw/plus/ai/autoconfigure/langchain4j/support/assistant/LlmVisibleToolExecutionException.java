package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

/**
 * 「抛出消息给 LLM」语义标记异常：工具方法内部抛出本异常时，其消息作为工具执行结果
 * 回传给 LLM，由 LLM 向用户解释并继续对话，而不是炸给业务调用方。
 *
 * <p><b>两版本行为</b>（语义等价，本类两边通用、零适配）：</p>
 * <ul>
 * <li>dev（0.35）：{@code DefaultToolExecutor} catch {@code InvocationTargetException}
 * 后 {@code return cause.getMessage()}——本异常消息天然回给 LLM；{@code from(String)}
 * 强制消息非空，从源头消除 0.35 下消息为 null 时 {@code ToolExecution.result(null)}
 * 直存的风险。</li>
 * <li>springboot3（1.20.2）：普通异常默认走 {@code ToolExecutionResult(isError=true,
 * errorMessage(cause))} 回 LLM（null 消息由类名兜底），本异常作为纯语义标记照常抛出即可，
 * 无需适配。</li>
 * </ul>
 */
public class LlmVisibleToolExecutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private LlmVisibleToolExecutionException(String message) {
		super(message);
	}

	/**
	 * 构造回传给 LLM 的可见异常，消息强制非空（去除首尾空白后不可为空）。
	 * @param message 回给 LLM 的解释文本
	 */
	public static LlmVisibleToolExecutionException from(String message) {
		if (message == null || message.trim().isEmpty()) {
			throw new IllegalArgumentException("回传给 LLM 的消息不能为空");
		}
		return new LlmVisibleToolExecutionException(message);
	}

}
