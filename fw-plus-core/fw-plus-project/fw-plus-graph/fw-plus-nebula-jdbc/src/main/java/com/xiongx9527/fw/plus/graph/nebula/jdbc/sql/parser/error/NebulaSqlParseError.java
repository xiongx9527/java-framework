package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error;

public class NebulaSqlParseError {

	private final ErrorKind kind;
	private final String message;
	private final String matrixId;
	private final String druidAstType;
	private final Integer line;
	private final Integer column;
	private final String sqlSnippet;

	public NebulaSqlParseError(ErrorKind kind, String message, String matrixId, String druidAstType,
							  Integer line, Integer column, String sqlSnippet) {
		this.kind = kind;
		this.message = message;
		this.matrixId = matrixId;
		this.druidAstType = druidAstType;
		this.line = line;
		this.column = column;
		this.sqlSnippet = sqlSnippet;
	}

	public ErrorKind getKind() {
		return kind;
	}

	public String getMessage() {
		return message;
	}

	public String getMatrixId() {
		return matrixId;
	}

	public String getDruidAstType() {
		return druidAstType;
	}

	public Integer getLine() {
		return line;
	}

	public Integer getColumn() {
		return column;
	}

	public String getSqlSnippet() {
		return sqlSnippet;
	}
}
