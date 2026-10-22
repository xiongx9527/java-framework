package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class NebulaSqlParseException extends BizException {

	/** 状态码 50009002（SERVER/DATABASE/002 nebula SQL 解析错误） */
	public static final int CODE = 50_009_002;

	private final NebulaSqlParseError error;

	public NebulaSqlParseException(NebulaSqlParseError error) {
		super(CODE, error.getMessage());
		this.error = error;
	}

	public NebulaSqlParseError getError() {
		return error;
	}
}
