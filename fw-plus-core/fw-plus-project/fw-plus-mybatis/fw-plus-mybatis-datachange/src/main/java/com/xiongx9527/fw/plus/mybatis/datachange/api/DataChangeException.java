package com.xiongx9527.fw.plus.mybatis.datachange.api;

public class DataChangeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final DataChangeErrorCode code;

	public DataChangeException(DataChangeErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public DataChangeException(DataChangeErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public DataChangeErrorCode getCode() { return code; }
}
