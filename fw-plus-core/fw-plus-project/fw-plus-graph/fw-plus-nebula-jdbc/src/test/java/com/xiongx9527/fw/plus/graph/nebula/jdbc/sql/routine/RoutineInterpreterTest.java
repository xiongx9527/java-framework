package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaParameterBindings;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutor;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
		assertEquals("Q:SELECT * FROM person WHERE id = ?", executor.calls.get(0));
		assertEquals("U:UPDATE person SET touched = 1 WHERE id = ?", executor.calls.get(1));
		assertEquals("Q:SELECT * FROM audit WHERE id = ?", executor.calls.get(2));
		assertEquals("v1", executor.bindings.get(0).get(1).getValue());
		assertEquals("v1", executor.bindings.get(1).get(1).getValue());
		assertEquals("v1", executor.bindings.get(2).get(1).getValue());
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
	public void statementBudgetShouldCountSqlInsideMatchedIfBranch() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 1);
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN IF 1 = 1 THEN SELECT * FROM person; UPDATE person SET touched = 1 WHERE id = 'v1'; ELSE SELECT * FROM fallback; END IF; END");
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));

		try {
			service.call("graph_a", "alice", "CALL p()", null, executor);
			fail("Nested IF body statements must count toward statement budget");
		}
		catch (SQLException exception) {
			assertEquals(1317, exception.getErrorCode());
			assertEquals("70100", exception.getSQLState());
			assertEquals(0, executor.calls.size());
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

	@Test
	public void callShouldBindParametersVariablesAndExecuteOnlyMatchedIfBranch() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p(IN mode VARCHAR(8), IN id_value VARCHAR(64)) BEGIN DECLARE target VARCHAR(64) DEFAULT 'missing'; SET target = id_value; IF mode = 'skip' THEN SELECT * FROM skipped WHERE id = target; ELSEIF target = 'v1' THEN SELECT * FROM person WHERE id = target; ELSE UPDATE person SET touched = 1 WHERE id = target; END IF; END");
		ResultSet resultSet = mock(ResultSet.class);
		RecordingExecutor executor = new RecordingExecutor(resultSet);

		RoutineCallResult result = service.call("graph_a", "alice", "CALL p('read', 'v1')", null, executor);

		assertTrue(result.hasResultSet());
		assertSame(resultSet, result.getResultSet());
		assertEquals(1, executor.calls.size());
		assertEquals("Q:SELECT * FROM person WHERE id = ?", executor.calls.get(0));
		assertEquals("v1", executor.bindings.get(0).get(1).getValue());
		assertEquals(Types.VARCHAR, executor.bindings.get(0).get(1).getJdbcType());
	}

	@Test
	public void executionTimeoutShouldRejectCallBeforeRunningBodySql() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 64, 0L);
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN SELECT * FROM person; END");
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));

		try {
			service.call("graph_a", "alice", "CALL p()", null, executor);
			fail("Timeout budget must fail");
		}
		catch (SQLException exception) {
			assertEquals(1317, exception.getErrorCode());
			assertEquals("70100", exception.getSQLState());
			assertEquals(0, executor.calls.size());
		}
	}

	@Test
	public void executionTimeoutShouldRejectCallAfterSlowBodySqlReturns() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 64, 100L);
		service.create("graph_a", "alice",
				"CREATE PROCEDURE p() BEGIN SELECT * FROM person; END");
		RecordingExecutor executor = new RecordingExecutor(mock(ResultSet.class));
		executor.queryDelayMillis = 150L;

		try {
			service.call("graph_a", "alice", "CALL p()", null, executor);
			fail("Timeout budget must be checked after a body SQL returns");
		}
		catch (SQLException exception) {
			assertEquals(1317, exception.getErrorCode());
			assertEquals("70100", exception.getSQLState());
			assertEquals(1, executor.calls.size());
		}
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final List<String> calls = new ArrayList<String>();
		private final List<NebulaParameterBindings> bindings = new ArrayList<NebulaParameterBindings>();
		private final ResultSet[] queryResults;
		private int queryIndex;
		private boolean failOnUpdate;
		private long queryDelayMillis;

		private RecordingExecutor(ResultSet... queryResults) {
			this.queryResults = queryResults;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			calls.add("Q:" + sql);
			this.bindings.add(bindings);
			if (queryDelayMillis > 0L) {
				try {
					Thread.sleep(queryDelayMillis);
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new SQLException("interrupted", exception);
				}
			}
			if (queryIndex < queryResults.length) {
				return queryResults[queryIndex++];
			}
			return null;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			calls.add("U:" + sql);
			this.bindings.add(bindings);
			if (failOnUpdate) {
				throw new SQLException("boom");
			}
			return 1;
		}
	}
}
