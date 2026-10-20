package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class RowPermissionHandleException extends BizException {
	private static final long serialVersionUID = 1L;

	public RowPermissionHandleException(String message) {
		super(message);
	}

	public RowPermissionHandleException(String message, Throwable cause) {
		super(message, cause);
	}
}
