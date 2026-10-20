package com.xiongx9527.fw.plus.mybatis.datachange.api;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class DataChangeException extends BizException {
	private static final long serialVersionUID = 1L;
	private final DataChangeErrorCode errorCode;

	public DataChangeException(DataChangeErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public DataChangeException(DataChangeErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/** 数据变更错误码；8 位统一状态码由父类 {@link BizException#getCode()} 提供 */
	public DataChangeErrorCode getErrorCode() { return errorCode; }
}
