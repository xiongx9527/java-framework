package com.xiongx9527.fw.plus.ai.askdata.model;

/**
 * 模型路由或模型执行失败的不可变结果。
 *
 * <p>Core 使用稳定错误码区分基础设施故障和不可重试拒绝，远程 Java 17 适配服务应将其
 * 自身异常映射为此语义，而不是将具体 SDK 或网络异常泄漏给调用方。</p>
 */
public final class AskDataModelResult {

	private final String modelKey;
	private final AskDataModelRoutePolicy routePolicy;
	private final String errorCode;
	private final String errorMessage;
	private final boolean retryable;

	private AskDataModelResult(String modelKey, AskDataModelRoutePolicy routePolicy, String errorCode,
			String errorMessage, boolean retryable) {
		this.modelKey = modelKey;
		this.routePolicy = routePolicy;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.retryable = retryable;
	}

	public static AskDataModelResult routed(String modelKey, AskDataModelRoutePolicy routePolicy) {
		if (modelKey == null || modelKey.length() == 0 || routePolicy == null) {
			throw new IllegalArgumentException("modelKey 和 routePolicy 不能为空");
		}
		return new AskDataModelResult(modelKey, routePolicy, null, null, true);
	}

	public static AskDataModelResult infrastructureFailure(String modelKey, String message) {
		return failure(modelKey, "MODEL_INFRASTRUCTURE_FAILURE", message, true);
	}

	public static AskDataModelResult nonRetryableFailure(String modelKey, String message) {
		return failure(modelKey, "MODEL_REQUEST_REJECTED", message, false);
	}

	private static AskDataModelResult failure(String modelKey, String errorCode, String message, boolean retryable) {
		if (modelKey == null || modelKey.length() == 0 || message == null || message.length() == 0) {
			throw new IllegalArgumentException("modelKey 和 message 不能为空");
		}
		return new AskDataModelResult(modelKey, null, errorCode, message, retryable);
	}

	public boolean isRouted() {
		return errorCode == null;
	}

	public String getModelKey() {
		return modelKey;
	}

	public AskDataModelRoutePolicy getRoutePolicy() {
		return routePolicy;
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
