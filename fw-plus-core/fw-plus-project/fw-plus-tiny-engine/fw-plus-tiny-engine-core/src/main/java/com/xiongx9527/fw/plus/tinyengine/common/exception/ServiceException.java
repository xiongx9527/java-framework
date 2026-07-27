package com.xiongx9527.fw.plus.tinyengine.common.exception;

public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String code;

	public ServiceException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
