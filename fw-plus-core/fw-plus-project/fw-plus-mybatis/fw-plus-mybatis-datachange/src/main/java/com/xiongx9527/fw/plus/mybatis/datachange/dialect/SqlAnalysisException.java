package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

public final class SqlAnalysisException extends DataChangeException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50019002（SERVER/CDC_STREAM/002 数据变更 SQL 解析失败） */
	public static final int CODE = 50_019_002;

	public SqlAnalysisException(String message) {
		super(CODE, DataChangeErrorCode.UNSUPPORTED_SQL, message);
	}

	public SqlAnalysisException(String message, Throwable cause) {
		super(CODE, DataChangeErrorCode.UNSUPPORTED_SQL, message, cause);
	}
}
