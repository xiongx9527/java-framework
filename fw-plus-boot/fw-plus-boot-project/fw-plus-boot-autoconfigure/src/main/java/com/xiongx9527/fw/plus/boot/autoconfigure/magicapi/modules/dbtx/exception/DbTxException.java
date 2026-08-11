package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxOutcome;
import com.xiongx9527.fw.plus.common.exception.BizException;

public class DbTxException extends BizException {

	private static final long serialVersionUID = 1L;

	private final DbTxErrorCode dbTxCode;

	private final DbTxOutcome outcome;

	public DbTxException(DbTxErrorCode code, String message) {
		super(message);
		this.dbTxCode = code;
		this.outcome = null;
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause) {
		this(code, message, cause, null);
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause, DbTxOutcome outcome) {
		super(message, cause);
		this.dbTxCode = code;
		this.outcome = outcome;
	}

	public DbTxErrorCode getDbTxCode() {
		return dbTxCode;
	}

	public DbTxOutcome getOutcome() {
		return outcome;
	}
}
