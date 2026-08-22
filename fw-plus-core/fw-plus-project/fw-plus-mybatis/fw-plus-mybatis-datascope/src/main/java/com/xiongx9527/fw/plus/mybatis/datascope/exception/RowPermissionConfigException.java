package com.xiongx9527.fw.plus.mybatis.datascope.exception;

public class RowPermissionConfigException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RowPermissionConfigException(String message) {
		super(message);
	}

	public RowPermissionConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
