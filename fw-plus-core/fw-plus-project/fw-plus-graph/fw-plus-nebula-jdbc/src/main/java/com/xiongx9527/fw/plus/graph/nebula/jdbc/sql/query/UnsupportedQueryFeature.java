package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

public enum UnsupportedQueryFeature {
	JOIN(NebulaQueryErrorCode.UNSUPPORTED_JOIN),
	SUBQUERY(NebulaQueryErrorCode.UNSUPPORTED_SUBQUERY),
	UNION(NebulaQueryErrorCode.UNSUPPORTED_UNION),
	FUNCTION(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION);

	private final NebulaQueryErrorCode errorCode;

	UnsupportedQueryFeature(NebulaQueryErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	public NebulaQueryErrorCode getErrorCode() {
		return errorCode;
	}
}
