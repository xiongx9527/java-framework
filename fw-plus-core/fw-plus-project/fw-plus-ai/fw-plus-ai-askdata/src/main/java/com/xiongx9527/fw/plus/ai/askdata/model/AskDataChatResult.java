package com.xiongx9527.fw.plus.ai.askdata.model;

/** 已脱敏的模型结果。 */
public final class AskDataChatResult {
	public static final String MODEL_ERROR = "MODEL_ERROR";
	public static final String MODEL_INFRASTRUCTURE_FAILURE = "MODEL_INFRASTRUCTURE_FAILURE";
	public static final String MODEL_REQUEST_REJECTED = "MODEL_REQUEST_REJECTED";

	private final String answer;
	private final String actualModelKey;
	private final String errorCode;
	private final String errorMessage;
	private final boolean retryable;
	private AskDataChatResult(String answer, String actualModelKey, String errorCode, String errorMessage,
			boolean retryable) {
		this.answer = answer;
		this.actualModelKey = actualModelKey;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.retryable = retryable;
	}
	public static AskDataChatResult success(String answer) {
		return new AskDataChatResult(answer, null, null, null, false);
	}
	public static AskDataChatResult success(String answer, String actualModelKey) {
		return new AskDataChatResult(answer, required(actualModelKey, "actualModelKey"), null, null, false);
	}
	public static AskDataChatResult failure(String actualModelKey, String errorCode, String errorMessage,
			boolean retryable) {
		return new AskDataChatResult(null, required(actualModelKey, "actualModelKey"),
				required(errorCode, "errorCode"), required(errorMessage, "errorMessage"), retryable);
	}
	public String getAnswer() { return answer; }
	public String getActualModelKey() { return actualModelKey; }
	public boolean isSuccess() { return errorCode == null; }
	public String getErrorCode() { return errorCode; }
	public String getErrorMessage() { return errorMessage; }
	public boolean isRetryable() { return retryable; }

	private static String required(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " 不能为空");
		}
		return value.trim();
	}
}
