package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;

/**
 * 一次 Core Embedding 调用的厂商中立结果。
 *
 * <p>成功结果携带向量、实际模型 key 和可选用量；失败结果只暴露稳定错误码与是否可重试，禁止泄漏
 * Provider SDK 异常、端点或凭据。只有 {@link #MODEL_INFRASTRUCTURE_FAILURE} 且标记可重试的结果，
 * 才允许默认 Flow 在无副作用的 Embedding 节点切换候选。</p>
 */
public final class AskDataEmbeddingResult {

	public static final String MODEL_INFRASTRUCTURE_FAILURE = "MODEL_INFRASTRUCTURE_FAILURE";

	private final float[] vector;
	private final String actualModelKey;
	private final AskDataModelUsage usage;
	private final String errorCode;
	private final String errorMessage;
	private final boolean retryable;

	private AskDataEmbeddingResult(float[] vector, String actualModelKey, AskDataModelUsage usage,
			String errorCode, String errorMessage, boolean retryable) {
		this.vector = vector == null ? null : vector.clone();
		this.actualModelKey = normalize(actualModelKey);
		this.usage = usage == null ? AskDataModelUsage.unknown() : usage;
		this.errorCode = normalize(errorCode);
		this.errorMessage = normalize(errorMessage);
		this.retryable = retryable;
	}

	public static AskDataEmbeddingResult success(float[] vector, String actualModelKey) {
		return success(vector, actualModelKey, AskDataModelUsage.unknown());
	}

	public static AskDataEmbeddingResult success(float[] vector, String actualModelKey,
			AskDataModelUsage usage) {
		if (vector == null || vector.length == 0 || normalize(actualModelKey) == null) {
			throw new IllegalArgumentException("vector 和 actualModelKey 不能为空");
		}
		return new AskDataEmbeddingResult(vector, actualModelKey, usage, null, null, false);
	}

	public static AskDataEmbeddingResult failure(String errorCode, String errorMessage, boolean retryable) {
		if (normalize(errorCode) == null || normalize(errorMessage) == null) {
			throw new IllegalArgumentException("errorCode 和 errorMessage 不能为空");
		}
		return new AskDataEmbeddingResult(null, null, AskDataModelUsage.unknown(), errorCode, errorMessage, retryable);
	}

	public boolean isSuccess() { return errorCode == null; }
	public float[] getVector() { return vector == null ? null : vector.clone(); }
	public String getActualModelKey() { return actualModelKey; }
	public AskDataModelUsage getUsage() { return usage; }
	public String getErrorCode() { return errorCode; }
	public String getErrorMessage() { return errorMessage; }
	public boolean isRetryable() { return retryable; }

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
