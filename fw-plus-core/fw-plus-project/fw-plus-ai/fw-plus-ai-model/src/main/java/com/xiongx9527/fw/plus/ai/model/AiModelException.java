package com.xiongx9527.fw.plus.ai.model;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class AiModelException extends BizException {

	public static final String MODEL_ERROR = "MODEL_ERROR";

	public static final String MODEL_INFRASTRUCTURE_FAILURE = "MODEL_INFRASTRUCTURE_FAILURE";

	public static final String MODEL_REQUEST_REJECTED = "MODEL_REQUEST_REJECTED";

	private final String errorCode;

	private final boolean retryable;

    public AiModelException(String message) {
		this(MODEL_ERROR, false, message, null);
    }

    public AiModelException(String message, Throwable cause) {
		this(MODEL_ERROR, false, message, cause);
	}

	public AiModelException(String errorCode, boolean retryable, String message, Throwable cause) {
		super(message, cause);
		if (errorCode == null || errorCode.trim().isEmpty()) {
			throw new IllegalArgumentException("errorCode cannot be blank");
		}
		this.errorCode = errorCode;
		this.retryable = retryable;
	}

	public static AiModelException infrastructureFailure(String message, Throwable cause) {
		return new AiModelException(MODEL_INFRASTRUCTURE_FAILURE, true, message, cause);
	}

	public static AiModelException requestRejected(String message, Throwable cause) {
		return new AiModelException(MODEL_REQUEST_REJECTED, false, message, cause);
	}

	public String getErrorCode() {
		return errorCode;
	}

	public boolean isRetryable() {
		return retryable;
    }
}
