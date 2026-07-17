package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import org.junit.After;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class NebulaStatementExecutorPathTest {

	@After
	public void tearDown() {
		NebulaSqlExecutorProvider.reset();
	}

	@Test
	public void executeQueryShouldDelegateSqlAndEmptyBindingsToExecutor() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		ResultSet resultSet = statement.executeQuery("SELECT name FROM player");

		assertSame(executor.resultSet, resultSet);
		assertEquals("SELECT name FROM player", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	@Test
	public void executeShouldDelegateQuerySqlAndEmptyBindingsToExecutor() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		boolean hasResultSet = statement.execute("SELECT name FROM player");

		assertTrue(hasResultSet);
		assertSame(executor.resultSet, statement.getResultSet());
		assertEquals("SELECT name FROM player", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	@Test
	public void executeShouldDelegateUpdateSqlAndReturnFalse() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		boolean hasResultSet = statement.execute("INSERT VERTEX player(name) VALUES 1:(\"Tim\")");

		assertEquals(false, hasResultSet);
		assertEquals("INSERT VERTEX player(name) VALUES 1:(\"Tim\")", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet = mock(ResultSet.class);
		private NebulaConnection connection;
		private String sql;
		private NebulaParameterBindings bindings;

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			this.connection = connection;
			this.sql = sql;
			this.bindings = bindings;
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			this.connection = connection;
			this.sql = sql;
			this.bindings = bindings;
			return 1;
		}
	}
}
