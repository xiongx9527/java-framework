package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

public final class JdbcStatementResult {
	public enum Type {
		RESULT_SET,
		UPDATE_COUNT
	}

	private final Type type;
	private final int updateCount;

	private JdbcStatementResult(Type type, int updateCount) {
		this.type = type;
		this.updateCount = updateCount;
	}

	public static JdbcStatementResult resultSet() {
		return new JdbcStatementResult(Type.RESULT_SET, -1);
	}

	public static JdbcStatementResult updateCount(int updateCount) {
		return new JdbcStatementResult(Type.UPDATE_COUNT, updateCount);
	}

	public Type getType() { return type; }
	public int getUpdateCount() { return updateCount; }
	public boolean isUpdateCount() { return type == Type.UPDATE_COUNT; }
}
