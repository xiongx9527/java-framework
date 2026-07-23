package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser;

public class ParserDiagnostic {

	private final String parserType;
	private final String sqlSnippet;
	private final boolean rawFallback;
	private final String matrixId;

	public ParserDiagnostic(String parserType, String sqlSnippet, boolean rawFallback, String matrixId) {
		this.parserType = parserType;
		this.sqlSnippet = sqlSnippet;
		this.rawFallback = rawFallback;
		this.matrixId = matrixId;
	}

	public String getParserType() {
		return parserType;
	}

	public String getSqlSnippet() {
		return sqlSnippet;
	}

	public boolean isRawFallback() {
		return rawFallback;
	}

	public String getMatrixId() {
		return matrixId;
	}
}
