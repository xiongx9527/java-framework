package com.xiongx9527.fw.plus.ai.askdata.model;

public class AskDataModelException extends RuntimeException {

	private final String errorCode;

	public AskDataModelException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
