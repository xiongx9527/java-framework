package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

public final class SqlAnalysisException extends DataChangeException {
	private static final long serialVersionUID = 1L;

	public SqlAnalysisException(String message) {
		super(DataChangeErrorCode.UNSUPPORTED_SQL, message);
	}

	public SqlAnalysisException(String message, Throwable cause) {
		super(DataChangeErrorCode.UNSUPPORTED_SQL, message, cause);
	}
}
