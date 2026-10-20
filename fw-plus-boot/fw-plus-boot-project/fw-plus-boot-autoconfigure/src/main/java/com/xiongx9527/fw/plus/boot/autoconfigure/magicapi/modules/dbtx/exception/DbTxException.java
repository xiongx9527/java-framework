package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxOutcome;
import com.xiongx9527.fw.plus.common.exception.BizException;

public class DbTxException extends BizException {

	private static final long serialVersionUID = 1L;

	/** 状态码 50009009（SERVER/DATABASE/009 动态事务执行失败） */
	public static final int CODE = 50_009_009;

	private final DbTxErrorCode dbTxCode;

	private final DbTxOutcome outcome;

	public DbTxException(DbTxErrorCode code, String message) {
		super(CODE, message);
		this.dbTxCode = code;
		this.outcome = null;
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause) {
		this(code, message, cause, null);
	}

	public DbTxException(DbTxErrorCode code, String message, Throwable cause, DbTxOutcome outcome) {
		super(CODE, message, cause);
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
