package com.xiongx9527.fw.plus.ai.askdata.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 结果映射插件使用的不可变结构化结果。
 *
 * <p>执行状态、实际读取行数、行数上限和超时设置属于已经记录的执行事实。映射插件可以替换列与行，
 * 但插件链会拒绝任何执行事实变化及扩大结果行数的输出。</p>
 */
public final class AskDataSqlResult {

	private final List<AskDataSqlColumn> columns;
	private final List<Map<String, Object>> rows;
	private final AskDataSqlExecutionStatus executionStatus;
	private final int executedRowCount;
	private final int rowLimit;
	private final long timeoutMillis;

	private AskDataSqlResult(List<AskDataSqlColumn> columns, List<Map<String, Object>> rows,
			AskDataSqlExecutionStatus executionStatus, int executedRowCount, int rowLimit, long timeoutMillis) {
		if (columns == null || rows == null || executionStatus == null) {
			throw new IllegalArgumentException("columns、rows 和 executionStatus 不能为空");
		}
		if (executedRowCount < 0 || rowLimit < 0 || timeoutMillis < 0L) {
			throw new IllegalArgumentException("执行行数、行数上限和超时不能为负数");
		}
		this.columns = copyColumns(columns);
		this.rows = copyRows(rows);
		this.executionStatus = executionStatus;
		this.executedRowCount = executedRowCount;
		this.rowLimit = rowLimit;
		this.timeoutMillis = timeoutMillis;
	}

	public static AskDataSqlResult of(List<AskDataSqlColumn> columns, List<Map<String, Object>> rows,
			AskDataSqlExecutionStatus executionStatus, int executedRowCount, int rowLimit, long timeoutMillis) {
		return new AskDataSqlResult(columns, rows, executionStatus, executedRowCount, rowLimit, timeoutMillis);
	}

	public List<AskDataSqlColumn> getColumns() {
		return columns;
	}

	public List<Map<String, Object>> getRows() {
		return rows;
	}

	public AskDataSqlExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	public int getExecutedRowCount() {
		return executedRowCount;
	}

	public int getRowLimit() {
		return rowLimit;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}

	public AskDataSqlResult withColumns(List<AskDataSqlColumn> mappedColumns) {
		return new AskDataSqlResult(mappedColumns, rows, executionStatus, executedRowCount, rowLimit, timeoutMillis);
	}

	public AskDataSqlResult withRows(List<Map<String, Object>> mappedRows) {
		return new AskDataSqlResult(columns, mappedRows, executionStatus, executedRowCount, rowLimit, timeoutMillis);
	}

	public AskDataSqlResult withData(List<AskDataSqlColumn> mappedColumns, List<Map<String, Object>> mappedRows) {
		return new AskDataSqlResult(mappedColumns, mappedRows, executionStatus, executedRowCount, rowLimit, timeoutMillis);
	}

	private static List<AskDataSqlColumn> copyColumns(List<AskDataSqlColumn> source) {
		List<AskDataSqlColumn> copy = new ArrayList<AskDataSqlColumn>(source.size());
		for (AskDataSqlColumn column : source) {
			if (column == null) {
				throw new IllegalArgumentException("columns 不能包含 null");
			}
			copy.add(column);
		}
		return Collections.unmodifiableList(copy);
	}

	private static List<Map<String, Object>> copyRows(List<Map<String, Object>> source) {
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>(source.size());
		for (Map<String, Object> row : source) {
			if (row == null) {
				throw new IllegalArgumentException("rows 不能包含 null");
			}
			copy.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(row)));
		}
		return Collections.unmodifiableList(copy);
	}
}
