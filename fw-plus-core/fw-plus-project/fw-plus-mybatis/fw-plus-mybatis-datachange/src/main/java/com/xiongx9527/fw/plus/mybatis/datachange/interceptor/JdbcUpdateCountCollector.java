package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JdbcUpdateCountCollector {

	public List<JdbcStatementResult> collect(Statement statement, int firstUpdateCount) throws SQLException {
		List<JdbcStatementResult> results = new ArrayList<JdbcStatementResult>();
		if (firstUpdateCount != -1) {
			results.add(JdbcStatementResult.updateCount(firstUpdateCount));
		} else if (statement.getResultSet() != null) {
			results.add(JdbcStatementResult.resultSet());
		}
		while (true) {
			boolean resultSet = statement.getMoreResults();
			if (resultSet) {
				results.add(JdbcStatementResult.resultSet());
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
}
