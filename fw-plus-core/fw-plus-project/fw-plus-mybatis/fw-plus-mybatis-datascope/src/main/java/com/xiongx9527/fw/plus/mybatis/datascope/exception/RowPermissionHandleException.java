package com.xiongx9527.fw.plus.mybatis.datascope.exception;

public class RowPermissionHandleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RowPermissionHandleException(String message) {
		super(message);
	}

	public RowPermissionHandleException(String message, Throwable cause) {
		super(message, cause);
	}
}
