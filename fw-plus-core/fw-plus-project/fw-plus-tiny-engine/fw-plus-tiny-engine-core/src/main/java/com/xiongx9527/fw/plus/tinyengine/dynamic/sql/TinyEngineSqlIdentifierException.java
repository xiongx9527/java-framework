package com.xiongx9527.fw.plus.tinyengine.dynamic.sql;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class TinyEngineSqlIdentifierException extends BizException {

	private static final long serialVersionUID = 1L;
	private static final String SERVICE_CODE = "TE_SQL_IDENTIFIER_INVALID";

	public TinyEngineSqlIdentifierException(String message) {
		super(message);
	}

	public String getServiceCode() {
		return SERVICE_CODE;
	}
}
