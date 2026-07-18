package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.sql.routine.RoutineService;
import org.junit.After;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaStatementRoutineTest {

	@After
	public void tearDown() {
		RoutineService.resetDefaultRepository();
	}

	@Test
	public void executeShouldRouteCreateCallAndDropProcedure() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));
		NebulaStatementImpl statement = new NebulaStatementImpl(connection(), executor);

		assertFalse(statement.execute("CREATE PROCEDURE p() BEGIN SELECT * FROM person; END"));
		assertTrue(statement.execute("CALL p()"));
		assertSame(executor.resultSet, statement.getResultSet());
		assertFalse(statement.execute("DROP PROCEDURE p"));
		assertEquals(1, executor.calls.size());
		assertEquals("Q:SELECT * FROM person", executor.calls.get(0));
	}

	@Test
	public void executeQueryShouldAllowCallReturningResultSet() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));
		NebulaStatementImpl statement = new NebulaStatementImpl(connection(), executor);
		statement.executeUpdate("CREATE PROCEDURE p() BEGIN SELECT * FROM person; END");

		ResultSet resultSet = statement.executeQuery("CALL p()");

		assertSame(executor.resultSet, resultSet);
	}

	@Test
	public void executeUpdateShouldAllowCreateDropAndUpdateOnlyCall() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));
		NebulaStatementImpl statement = new NebulaStatementImpl(connection(), executor);

		assertEquals(0, statement.executeUpdate("CREATE PROCEDURE p() BEGIN UPDATE person SET touched = 1 WHERE id = 'v1'; END"));
		assertEquals(1, statement.executeUpdate("CALL p()"));
		assertEquals(0, statement.executeUpdate("DROP PROCEDURE p"));
	}

	@Test
	public void ordinarySelectShouldKeepOriginalExecutorPath() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));
		NebulaStatementImpl statement = new NebulaStatementImpl(connection(), executor);

		assertTrue(statement.execute("SELECT * FROM person"));

		assertSame(executor.resultSet, statement.getResultSet());
		assertEquals(1, executor.calls.size());
		assertEquals("Q:SELECT * FROM person", executor.calls.get(0));
	}

	private NebulaConnection connection() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.getSchema()).thenReturn("graph_a");
		return connection;
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet;
		private final List<String> calls = new ArrayList<String>();

		private RecordingExecutor(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			calls.add("Q:" + sql);
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			calls.add("U:" + sql);
			return 1;
		}
	}
}
