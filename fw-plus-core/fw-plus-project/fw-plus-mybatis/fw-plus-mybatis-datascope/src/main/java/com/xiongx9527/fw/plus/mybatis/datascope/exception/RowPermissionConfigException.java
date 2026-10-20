package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class RowPermissionConfigException extends BizException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50003001（SERVER/AUTHORIZATION/001 行级数据权限配置解析失败） */
	public static final int CODE = 50_003_001;

	public RowPermissionConfigException(String message) {
		super(CODE, message);
	}

	public RowPermissionConfigException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
