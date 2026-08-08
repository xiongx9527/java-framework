package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JdbcStatementResult {
	public enum Type {
		RESULT_SET,
		UPDATE_COUNT
	}

	private final Type type;
	private final int updateCount;
	private final boolean capturedRows;
	private final List<Map<String, Object>> rows;

	private JdbcStatementResult(Type type, int updateCount, boolean capturedRows,
			List<Map<String, Object>> rows) {
		this.type = type;
		this.updateCount = updateCount;
		this.capturedRows = capturedRows;
		this.rows = immutableRows(rows);
	}

	public static JdbcStatementResult resultSet() {
		return new JdbcStatementResult(Type.RESULT_SET, -1, false,
				Collections.<Map<String, Object>>emptyList());
	}

	public static JdbcStatementResult resultSet(List<Map<String, Object>> rows) {
		return new JdbcStatementResult(Type.RESULT_SET, -1, true, rows);
	}

	public static JdbcStatementResult updateCount(int updateCount) {
		return new JdbcStatementResult(Type.UPDATE_COUNT, updateCount, false,
				Collections.<Map<String, Object>>emptyList());
	}

	public Type getType() { return type; }
	public int getUpdateCount() { return updateCount; }
	public boolean hasCapturedRows() { return capturedRows; }
	public List<Map<String, Object>> getRows() { return rows; }
	public boolean isUpdateCount() { return type == Type.UPDATE_COUNT; }

	private static List<Map<String, Object>> immutableRows(List<Map<String, Object>> values) {
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
		if (values != null) {
			for (Map<String, Object> value : values) {
				copy.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value)));
			}
		}
		return Collections.unmodifiableList(copy);
	}
}
