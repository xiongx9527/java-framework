package com.xiongx9527.fw.plus.graph.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphJdbcResult {

	private final List<String> columns;
	private final List<Map<String, Object>> rows;
	private final int updateCount;
	private final boolean resultSet;

	private GraphJdbcResult(List<String> columns, List<Map<String, Object>> rows, int updateCount, boolean resultSet) {
		this.columns = copyColumns(columns);
		this.rows = copyRows(rows);
		this.updateCount = updateCount;
		this.resultSet = resultSet;
	}

	public static GraphJdbcResult rows(List<String> columns, List<Map<String, Object>> rows) {
		return new GraphJdbcResult(columns, rows, -1, true);
	}

	public static GraphJdbcResult updateCount(int updateCount) {
		return new GraphJdbcResult(Collections.<String>emptyList(), Collections.<Map<String, Object>>emptyList(),
				updateCount, false);
	}

	public List<String> getColumns() {
		return copyColumns(columns);
	}

	public List<Map<String, Object>> getRows() {
		return copyRows(rows);
	}

	public int getUpdateCount() {
		return updateCount;
	}

	public boolean hasResultSet() {
		return resultSet;
	}

	private static List<String> copyColumns(List<String> columns) {
		return columns == null ? new ArrayList<String>() : new ArrayList<String>(columns);
	}

	private static List<Map<String, Object>> copyRows(List<Map<String, Object>> rows) {
		List<Map<String, Object>> copiedRows = new ArrayList<Map<String, Object>>();
		if (rows == null) {
			return copiedRows;
		}
		for (Map<String, Object> row : rows) {
			copiedRows.add(row == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(row));
		}
		return copiedRows;
	}
}
