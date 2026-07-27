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

	public static AskDataRemoteRuntimeResult success(String answer) {
		return new AskDataRemoteRuntimeResult(answer, null, null, false);
	}

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
