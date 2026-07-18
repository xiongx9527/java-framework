package com.vesoft.nebula.jdbc.sql.write;

import java.sql.SQLException;

public class NebulaWriteConversionException extends SQLException {
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
