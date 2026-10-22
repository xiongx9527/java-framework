package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class NebulaQueryConversionException extends BizException {

	/** 状态码 50009003（SERVER/DATABASE/003 nebula 查询结果转换失败） */
	public static final int CODE = 50_009_003;

	private final NebulaQueryErrorCode errorCode;

	public NebulaQueryConversionException(NebulaQueryErrorCode errorCode, String message) {
		super(CODE, errorCode.name() + ": " + message);
		this.errorCode = errorCode;
	}

	public static NebulaQueryConversionException of(NebulaQueryErrorCode errorCode, String message) {
		return new NebulaQueryConversionException(errorCode, message);
	}

	public NebulaQueryErrorCode getQueryErrorCode() {
		return errorCode;
	}
}
