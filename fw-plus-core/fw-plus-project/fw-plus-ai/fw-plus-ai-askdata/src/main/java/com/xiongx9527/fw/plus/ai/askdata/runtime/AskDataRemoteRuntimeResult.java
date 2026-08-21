package com.xiongx9527.fw.plus.ai.askdata.runtime;

/**
 * Java 17 独立运行时适配服务的无框架结果。
 *
 * <p>适配服务将网络超时等基础设施错误标记为可重试，将鉴权、参数等业务拒绝标记为不可重试；
 * Core 据此保留稳定错误映射和审计链路，而不依赖 Spring AI 或 AgentScope 的异常类型。</p>
 */
public final class AskDataRemoteRuntimeResult {

	private final String answer;
	private final String errorCode;
	private final String errorMessage;
	private final boolean retryable;

	private AskDataRemoteRuntimeResult(String answer, String errorCode, String errorMessage, boolean retryable) {
		this.answer = answer;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.retryable = retryable;
	}

	/**
	 * 创建远程运行时成功结果。
	 *
	 * <p>该对象是 Java 17 Remote Runtime 与 Core 之间的无框架边界，只传递已完成的回答；调用方仍须
	 * 使用请求中的项目、操作人和审计关联标识记录访问，不得从远程服务返回内容推导或覆盖隔离上下文。</p>
	 *
	 * @param answer 远程运行时返回的回答，可为 {@code null}
	 * @return 成功结果
	 */
	public static AskDataRemoteRuntimeResult success(String answer) {
		return new AskDataRemoteRuntimeResult(answer, null, null, false);
	}

	/**
	 * 创建远程运行时失败结果。
	 *
	 * <p>{@code retryable} 为 {@code true} 仅用于超时、网络、网关和限流等基础设施故障；鉴权、参数、
	 * 能力策略等业务拒绝必须为 {@code false}。Java 17 适配服务必须将 SDK 与传输异常映射为稳定错误码，
	 * 不得向 Core 或审计记录泄漏供应商异常详情。</p>
	 *
	 * @param errorCode 稳定且可审计的错误码，不能为空白
	 * @param errorMessage 已脱敏的受控错误信息，不能为空白
	 * @param retryable 是否允许 Core 选择后继能力或模型
	 * @return 失败结果
	 */
	public static AskDataRemoteRuntimeResult failure(String errorCode, String errorMessage, boolean retryable) {
		if (errorCode == null || errorCode.trim().length() == 0 || errorMessage == null || errorMessage.trim().length() == 0) {
			throw new IllegalArgumentException("errorCode 和 errorMessage 不能为空白");
		}
		return new AskDataRemoteRuntimeResult(null, errorCode.trim(), errorMessage.trim(), retryable);
	}

	public boolean isSuccess() {
		return errorCode == null;
	}

	public String getAnswer() {
		return answer;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
