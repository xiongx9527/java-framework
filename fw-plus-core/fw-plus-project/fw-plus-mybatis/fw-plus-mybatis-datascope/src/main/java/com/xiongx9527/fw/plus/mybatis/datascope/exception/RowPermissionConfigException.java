package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class RowPermissionConfigException extends BizException {
	private static final long serialVersionUID = 1L;

	public RowPermissionConfigException(String message) {
		super(message);
	}

	public RowPermissionConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
