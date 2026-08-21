package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class NebulaSqlParseException extends BizException {

	private final NebulaSqlParseError error;

	public NebulaSqlParseException(NebulaSqlParseError error) {
		super(error.getMessage());
		this.error = error;
	}

	public NebulaSqlParseError getError() {
		return error;
	}
}
