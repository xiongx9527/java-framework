package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class NebulaWriteConversionException extends BizException {

	/** 状态码 50009004（SERVER/DATABASE/004 nebula 写入转换失败） */
	public static final int CODE = 50_009_004;

	private final NebulaWriteErrorCode errorCode;

	public NebulaWriteConversionException(NebulaWriteErrorCode errorCode, String message) {
		super(CODE, errorCode.name() + ": " + message);
		this.errorCode = errorCode;
	}

	public static NebulaWriteConversionException of(NebulaWriteErrorCode errorCode, String message) {
		return new NebulaWriteConversionException(errorCode, message);
	}

	public NebulaWriteErrorCode getWriteErrorCode() {
		return errorCode;
	}
}
