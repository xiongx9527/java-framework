package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxOutcome;

public class DbTxException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final DbTxErrorCode code;

	private final DbTxOutcome outcome;

	public DbTxException(DbTxErrorCode code, String message) {
		super(message);
		this.code = code;
		this.outcome = null;
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause) {
		this(code, message, cause, null);
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause, DbTxOutcome outcome) {
		super(message, cause);
		this.code = code;
		this.outcome = outcome;
	}

	public DbTxErrorCode getCode() {
		return code;
	}

	public DbTxOutcome getOutcome() {
		return outcome;
	}
}
