package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class NebulaWriteConversionException extends BizException {
	private final NebulaWriteErrorCode errorCode;

	public NebulaWriteConversionException(NebulaWriteErrorCode errorCode, String message) {
		super(errorCode.name() + ": " + message);
		this.errorCode = errorCode;
	}

	public static NebulaWriteConversionException of(NebulaWriteErrorCode errorCode, String message) {
		return new NebulaWriteConversionException(errorCode, message);
	}

	public NebulaWriteErrorCode getWriteErrorCode() {
		return errorCode;
	}
}
