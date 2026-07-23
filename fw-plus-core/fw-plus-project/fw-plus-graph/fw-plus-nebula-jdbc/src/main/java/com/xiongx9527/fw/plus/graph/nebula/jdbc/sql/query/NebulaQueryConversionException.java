package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

import java.sql.SQLException;

public class NebulaQueryConversionException extends SQLException {
	private final NebulaQueryErrorCode errorCode;

	public NebulaQueryConversionException(NebulaQueryErrorCode errorCode, String message) {
		super(errorCode.name() + ": " + message);
		this.errorCode = errorCode;
	}

	public static NebulaQueryConversionException of(NebulaQueryErrorCode errorCode, String message) {
		return new NebulaQueryConversionException(errorCode, message);
	}

	public NebulaQueryErrorCode getQueryErrorCode() {
		return errorCode;
	}
}
