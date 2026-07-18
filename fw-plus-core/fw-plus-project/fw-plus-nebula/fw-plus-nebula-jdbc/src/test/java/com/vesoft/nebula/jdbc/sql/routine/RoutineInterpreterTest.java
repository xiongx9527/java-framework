package com.vesoft.nebula.jdbc.sql.routine;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class RoutineInterpreterTest {

	@Test
	public void callShouldExecuteBodyStatementsSequentiallyAndReturnLastResultSet() throws Exception {
		InMemoryRoutineMetadataRepository repository = new InMemoryRoutineMetadataRepository();
		RoutineService service = new RoutineService(repository);
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; UPDATE person SET touched = 1 WHERE id = id; SELECT * FROM audit WHERE id = id; END");
		ResultSet first = mock(ResultSet.class);
		ResultSet last = mock(ResultSet.class);
		RecordingExecutor executor = new RecordingExecutor(first, last);

		RoutineCallResult result = service.call("graph_a", "alice", "CALL p('v1')", null, executor);

		assertTrue(result.hasResultSet());
		assertSame(last, result.getResultSet());
		assertEquals(0, result.getUpdateCount());
		assertEquals("Q:SELECT * FROM person WHERE id = id", executor.calls.get(0));
		assertEquals("U:UPDATE person SET touched = 1 WHERE id = id", executor.calls.get(1));
		assertEquals("Q:SELECT * FROM audit WHERE id = id", executor.calls.get(2));
	}

	@Test
	public void updateOnlyCallShouldReturnOkSummary() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN UPDATE person SET touched = 1 WHERE id = 'v1'; END");
		RecordingExecutor executor = new RecordingExecutor();

		RoutineCallResult result = service.call("graph_a", "alice", "CALL p()", null, executor);

		assertFalse(result.hasResultSet());
		assertEquals(1, result.getUpdateCount());
	}

	@Test
	public void callShouldValidateArgumentCount() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT 1; END");

		try {
			service.call("graph_a", "alice", "CALL p()", null, new RecordingExecutor());
			fail("Argument mismatch must fail");
		}
		catch (SQLException exception) {
			assertEquals(1318, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
	}

	@Test
	public void firstSqlFailureShouldShortCircuitRemainingStatements() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN UPDATE person SET touched = 1 WHERE id = 'v1'; SELECT 1; END");
		RecordingExecutor executor = new RecordingExecutor();
		executor.failOnUpdate = true;

		try {
			service.call("graph_a", "alice", "CALL p()", null, executor);
			fail("SQL failure must propagate");
		}
		catch (SQLException exception) {
			assertEquals("boom", exception.getMessage());
			assertEquals(1, executor.calls.size());
		}
	}

	@Test
	public void statementBudgetShouldRejectLongBody() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 1);
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN UPDATE person SET touched = 1 WHERE id = 'v1'; SELECT 1; END");

		try {
			service.call("graph_a", "alice", "CALL p()", null, new RecordingExecutor());
			fail("Statement budget must fail");
		}
		catch (SQLException exception) {
			assertEquals(1317, exception.getErrorCode());
			assertEquals("70100", exception.getSQLState());
		}
	}

	@Test
	public void bodyCallShouldBeRejectedAsRecursiveBoundary() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN CALL p(); END");

		try {
			service.call("graph_a", "alice", "CALL p()", null, new RecordingExecutor());
			fail("Body CALL must fail");
		}
		catch (SQLException exception) {
			assertEquals(1235, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final List<String> calls = new ArrayList<String>();
		private final ResultSet[] queryResults;
		private int queryIndex;
		private boolean failOnUpdate;

		private RecordingExecutor(ResultSet... queryResults) {
			this.queryResults = queryResults;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			calls.add("Q:" + sql);
			if (queryIndex < queryResults.length) {
				return queryResults[queryIndex++];
			}
			return null;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			calls.add("U:" + sql);
			if (failOnUpdate) {
				throw new SQLException("boom");
			}
			return 1;
		}
	}
}
