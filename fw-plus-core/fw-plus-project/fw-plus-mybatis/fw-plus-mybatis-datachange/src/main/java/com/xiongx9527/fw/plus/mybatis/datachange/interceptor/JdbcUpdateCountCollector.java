package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JdbcUpdateCountCollector {

	public List<Integer> collect(Statement statement, int firstUpdateCount) throws SQLException {
		List<Integer> counts = new ArrayList<Integer>();
		if (firstUpdateCount != -1) {
			counts.add(Integer.valueOf(firstUpdateCount));
		}
		while (true) {
			boolean resultSet = statement.getMoreResults();
			int updateCount = statement.getUpdateCount();
			if (!resultSet && updateCount == -1) {
				break;
			}
			if (updateCount != -1) {
				counts.add(Integer.valueOf(updateCount));
			}
		}
		return Collections.unmodifiableList(counts);
	}
}
