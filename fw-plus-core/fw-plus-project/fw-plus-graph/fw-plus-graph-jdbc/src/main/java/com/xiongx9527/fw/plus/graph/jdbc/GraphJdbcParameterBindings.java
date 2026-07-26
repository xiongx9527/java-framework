package com.xiongx9527.fw.plus.graph.jdbc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphJdbcParameterBindings {

	private final Map<Integer, Object> values = new LinkedHashMap<Integer, Object>();

	public void bind(int parameterIndex, Object value) {
		if (parameterIndex < 1) {
			throw new IllegalArgumentException("parameterIndex must be greater than 0");
		}
		values.put(Integer.valueOf(parameterIndex), value);
	}

	public Object get(int parameterIndex) {
		return values.get(Integer.valueOf(parameterIndex));
	}

	public Map<Integer, Object> asMap() {
		return Collections.unmodifiableMap(values);
	}

	public void clear() {
		values.clear();
	}

	public int size() {
		return values.size();
	}

	public static int countQuestionMarks(String sql) {
		if (sql == null || sql.length() == 0) {
			return 0;
		}
		int count = 0;
		char quote = 0;
		for (int index = 0; index < sql.length(); index++) {
			char current = sql.charAt(index);
			if (quote != 0) {
				if (current == quote) {
					if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
						index++;
					}
					else {
						quote = 0;
					}
				}
				continue;
			}
			if (current == '\'' || current == '\"') {
				quote = current;
			}
			else if (current == '?') {
				count++;
			}
		}
		return count;
	}
}
