package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error;

public class NebulaSqlParseException extends Exception {

	private final NebulaSqlParseError error;

	public NebulaSqlParseException(NebulaSqlParseError error) {
		super(error.getMessage());
		this.error = error;
	}

	public NebulaSqlParseError getError() {
		return error;
	}
}
