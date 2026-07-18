package com.vesoft.nebula.jdbc.mysql.replication;

public class ReplicationError {
	private final int errorCode;
	private final String sqlState;
	private final String message;

	public ReplicationError(int errorCode, String sqlState, String message) {
		this.errorCode = errorCode;
		this.sqlState = sqlState;
		this.message = message;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getSqlState() {
		return sqlState;
	}

	public String getMessage() {
		return message;
	}
}
