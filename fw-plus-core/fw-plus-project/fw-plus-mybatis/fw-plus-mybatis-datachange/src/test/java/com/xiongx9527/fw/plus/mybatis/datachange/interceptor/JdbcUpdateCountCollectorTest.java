package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import org.junit.jupiter.api.Test;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcUpdateCountCollectorTest {

	@Test
	void collectsEveryJdbcUpdateCountInStatementOrder() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getMoreResults()).thenReturn(false, false);
		when(statement.getUpdateCount()).thenReturn(7, -1);

		assertThat(new JdbcUpdateCountCollector().collect(statement, 3)).containsExactly(3, 7);
	}

	@Test
	void preservesSuccessNoInfoWithoutGuessingAffectedRows() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getMoreResults()).thenReturn(false);
		when(statement.getUpdateCount()).thenReturn(-1);

		assertThat(new JdbcUpdateCountCollector().collect(statement, Statement.SUCCESS_NO_INFO))
				.containsExactly(Statement.SUCCESS_NO_INFO);
	}

	@Test
	void skipsResultSetsUntilTheNextUpdateCount() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getMoreResults()).thenReturn(true, false, false);
		when(statement.getUpdateCount()).thenReturn(-1, 5, -1);

		assertThat(new JdbcUpdateCountCollector().collect(statement, -1)).containsExactly(5);
	}
}
