package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JdbcUpdateCountCollector {

	public List<JdbcStatementResult> collect(Statement statement, int firstUpdateCount) throws SQLException {
		return collect(statement, firstUpdateCount, Collections.<String>emptyList(), 1);
	}

	public List<JdbcStatementResult> collect(Statement statement, int firstUpdateCount,
			List<String> returningColumns, int maxRows) throws SQLException {
		if (maxRows <= 0) { throw new IllegalArgumentException("maxRows must be greater than zero"); }
		try {
			return collectResults(statement, firstUpdateCount, returningColumns, maxRows);
		} catch (SQLException exception) {
			throw unavailable();
		} catch (RuntimeException exception) {
			throw unavailable();
		}
	}

	private List<JdbcStatementResult> collectResults(Statement statement, int firstUpdateCount,
			List<String> returningColumns, int maxRows) throws SQLException {
		List<JdbcStatementResult> results = new ArrayList<JdbcStatementResult>();
		if (firstUpdateCount != -1) {
			results.add(JdbcStatementResult.updateCount(firstUpdateCount));
		} else {
			ResultSet firstResultSet = statement.getResultSet();
			if (firstResultSet != null) {
				results.add(resultSet(firstResultSet, returningColumns, maxRows));
			}
		}
		while (true) {
			boolean resultSet = statement.getMoreResults();
			if (resultSet) {
				results.add(resultSet(statement.getResultSet(), returningColumns, maxRows));
				continue;
			}
			int updateCount = statement.getUpdateCount();
			if (updateCount == -1) {
				break;
			}
			results.add(JdbcStatementResult.updateCount(updateCount));
		}
		return Collections.unmodifiableList(results);
	}

	private SQLException unavailable() {
		return new SQLException("JDBC_STATEMENT_RESULTS_UNAVAILABLE");
	}

	private JdbcStatementResult resultSet(ResultSet resultSet, List<String> returningColumns, int maxRows)
			throws SQLException {
		if (returningColumns == null || returningColumns.isEmpty()) {
			return JdbcStatementResult.resultSet();
		}
		ResultSetMetaData metadata = resultSet.getMetaData();
		Map<String, Integer> indexes = new LinkedHashMap<String, Integer>();
		for (String column : returningColumns) {
			for (int index = 1; index <= metadata.getColumnCount(); index++) {
				String label = metadata.getColumnLabel(index);
				String name = metadata.getColumnName(index);
				if (column.equalsIgnoreCase(label) || column.equalsIgnoreCase(name)) {
					indexes.put(column, Integer.valueOf(index));
					break;
				}
			}
		}
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		while (resultSet.next()) {
			if (rows.size() >= maxRows) { throw new SQLException("RETURNING row limit exceeded"); }
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, Integer> entry : indexes.entrySet()) {
				row.put(entry.getKey(), resultSet.getObject(entry.getValue().intValue()));
			}
			rows.add(row);
		}
		return JdbcStatementResult.resultSet(rows);
	}
}
