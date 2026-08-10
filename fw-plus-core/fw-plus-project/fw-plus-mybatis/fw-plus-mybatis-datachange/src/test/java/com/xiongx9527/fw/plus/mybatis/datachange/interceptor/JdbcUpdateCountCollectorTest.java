package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcUpdateCountCollectorTest {

	@Test
	void collectsEveryJdbcUpdateCountInStatementOrder() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getMoreResults()).thenReturn(false, false);
		when(statement.getUpdateCount()).thenReturn(7, -1);

		List<JdbcStatementResult> results = new JdbcUpdateCountCollector().collect(statement, 3);

		assertThat(results).extracting(JdbcStatementResult::getType)
				.containsExactly(JdbcStatementResult.Type.UPDATE_COUNT, JdbcStatementResult.Type.UPDATE_COUNT);
		assertThat(results).extracting(JdbcStatementResult::getUpdateCount).containsExactly(3, 7);
	}

	@Test
	void preservesSuccessNoInfoWithoutGuessingAffectedRows() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getMoreResults()).thenReturn(false);
		when(statement.getUpdateCount()).thenReturn(-1);

		assertThat(new JdbcUpdateCountCollector().collect(statement, Statement.SUCCESS_NO_INFO))
				.singleElement().extracting(JdbcStatementResult::getUpdateCount)
				.isEqualTo(Statement.SUCCESS_NO_INFO);
	}

	@Test
	void preservesResultSetsBeforeTheNextUpdateCount() throws Exception {
		Statement statement = mock(Statement.class);
		when(statement.getResultSet()).thenReturn(mock(ResultSet.class));
		when(statement.getMoreResults()).thenReturn(false, false);
		when(statement.getUpdateCount()).thenReturn(5, -1);

		List<JdbcStatementResult> results = new JdbcUpdateCountCollector().collect(statement, -1);

		assertThat(results).extracting(JdbcStatementResult::getType)
				.containsExactly(JdbcStatementResult.Type.RESULT_SET, JdbcStatementResult.Type.UPDATE_COUNT);
		assertThat(results.get(1).getUpdateCount()).isEqualTo(5);
	}

	@Test
	void capturesOnlyRequestedReturningColumnsWithBoundedRows() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:returning_rows;DB_CLOSE_DELAY=-1");
			 Statement statement = connection.createStatement()) {
			statement.execute("SELECT CAST(42 AS BIGINT) AS id, 'not-captured' AS secret");

			List<JdbcStatementResult> results = new JdbcUpdateCountCollector().collect(statement, -1,
					Collections.singletonList("ID"), 1);

			assertThat(results).singleElement().satisfies(result -> {
				assertThat(result.hasCapturedRows()).isTrue();
				assertThat(result.getRows())
						.containsExactly(Collections.<String, Object>singletonMap("ID", 42L));
			});
		}
	}

	@Test
	void sanitizesRuntimeDriverFailureDuringReturningCapture() throws Exception {
		Statement statement = mock(Statement.class);
		ResultSet rows = mock(ResultSet.class);
		when(statement.getResultSet()).thenReturn(rows);
		when(rows.getMetaData()).thenThrow(new IllegalStateException("jdbc:secret-runtime"));

		assertThatThrownBy(() -> new JdbcUpdateCountCollector().collect(statement, -1,
				Collections.singletonList("ID"), 1))
				.isInstanceOf(java.sql.SQLException.class)
				.hasMessage("JDBC_STATEMENT_RESULTS_UNAVAILABLE")
				.hasNoCause();
	}

	@Test
	void rejectsReturningRowsBeyondConfiguredBound() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:returning_limit;DB_CLOSE_DELAY=-1");
			 Statement statement = connection.createStatement()) {
			statement.execute("SELECT id FROM (VALUES (1), (2)) returned(id)");

			assertThatThrownBy(() -> new JdbcUpdateCountCollector().collect(statement, -1,
					Collections.singletonList("ID"), 1))
					.isInstanceOf(java.sql.SQLException.class)
					.hasMessage("JDBC_STATEMENT_RESULTS_UNAVAILABLE")
					.hasNoCause();
		}
	}
}
