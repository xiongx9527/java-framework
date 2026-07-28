package com.xiongx9527.fw.plus.tinyengine.dynamic.sql;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;

public class TinyEngineSqlIdentifierException extends ServiceException {

	private static final long serialVersionUID = 1L;

	public TinyEngineSqlIdentifierException(String message) {
		super("TE_SQL_IDENTIFIER_INVALID", message);
	}
}
