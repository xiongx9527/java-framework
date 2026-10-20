package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class RowPermissionHandleException extends BizException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50003002（SERVER/AUTHORIZATION/002 行级数据权限处理失败） */
	public static final int CODE = 50_003_002;

	public RowPermissionHandleException(String message) {
		super(CODE, message);
	}

	public RowPermissionHandleException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
