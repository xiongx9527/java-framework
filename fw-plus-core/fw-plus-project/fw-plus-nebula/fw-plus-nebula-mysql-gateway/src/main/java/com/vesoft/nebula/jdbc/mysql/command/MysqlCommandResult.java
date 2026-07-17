package com.vesoft.nebula.jdbc.mysql.command;

import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;

public class MysqlCommandResult {
	public enum Type {
		OK,
		ERROR,
		RESULT_SET
	}

	private final Type type;
	private final MysqlQueryResult resultSet;
	private final int errorCode;
	private final String sqlState;
	private final String message;
	private final byte[] payload;

	private MysqlCommandResult(Type type, MysqlQueryResult resultSet, int errorCode, String sqlState, String message, byte[] payload) {
		this.type = type;
		this.resultSet = resultSet;
		this.errorCode = errorCode;
		this.sqlState = sqlState;
		this.message = message;
		this.payload = payload;
	}

	public static MysqlCommandResult ok(byte[] payload) {
		return new MysqlCommandResult(Type.OK, null, 0, null, null, payload);
	}

	public static MysqlCommandResult error(int errorCode, String sqlState, String message, byte[] payload) {
		return new MysqlCommandResult(Type.ERROR, null, errorCode, sqlState, message, payload);
	}

	public static MysqlCommandResult resultSet(MysqlQueryResult resultSet) {
		return new MysqlCommandResult(Type.RESULT_SET, resultSet, 0, null, null, null);
	}

	public Type getType() {
		return type;
	}

	public MysqlQueryResult getResultSet() {
		return resultSet;
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

	public byte[] getPayload() {
		return payload;
	}
}
