package com.vesoft.nebula.jdbc.framework;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.junit.After;
import org.junit.Test;

import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaFrameworkSqlShapeTest {

	@After
	public void tearDown() {
		NebulaSqlExecutorProvider.reset();
	}

	@Test
	public void preparedStatementShouldPassThroughFrameworkSqlShapes() throws Exception {
		String[] sqlShapes = new String[] {
				"SELECT 1",
				"SELECT COUNT(*) FROM player",
				"SELECT name AS playerName FROM player WHERE id = ?",
				"SELECT name FROM player WHERE id IN (?, ?)",
				"SELECT name FROM player WHERE age > ? AND active = ?",
				"SHOW TAGS",
				"DESCRIBE TAG player"
		};
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mockOpenConnection();

		for (String sql : sqlShapes) {
			PreparedStatement statement = connection.prepareStatement(sql);
			int placeholders = countPlaceholders(sql);
			bindPlaceholders(statement, placeholders);

			statement.executeQuery();

			assertEquals(sql, executor.getLastSql());
			assertEquals(placeholders, executor.getLastBindings().size());
		}
	}

	private NebulaConnection mockOpenConnection() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.isClosed()).thenReturn(false);
		when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
			String sql = invocation.getArgument(0);
			return new NebulaPreparedStatementImpl(connection, sql);
		});
		return connection;
	}

	private int countPlaceholders(String sql) {
		int count = 0;
		for (int i = 0; i < sql.length(); i++) {
			if (sql.charAt(i) == '?') {
				count++;
			}
		}
		return count;
	}

	private void bindPlaceholders(PreparedStatement statement, int placeholders) throws Exception {
		for (int i = 1; i <= placeholders; i++) {
			statement.setLong(i, 1000L + i);
		}
	}
}
