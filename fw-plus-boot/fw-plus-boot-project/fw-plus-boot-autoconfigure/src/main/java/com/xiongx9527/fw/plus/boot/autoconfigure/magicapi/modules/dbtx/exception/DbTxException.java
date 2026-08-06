package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception;

public class DbTxException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final DbTxErrorCode code;

	public DbTxException(DbTxErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public DbTxErrorCode getCode() {
		return code;
	}
}
