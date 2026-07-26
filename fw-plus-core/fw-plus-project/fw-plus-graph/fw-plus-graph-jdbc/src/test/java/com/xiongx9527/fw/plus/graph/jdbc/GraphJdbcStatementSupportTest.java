package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GraphJdbcStatementSupportTest {

	@Test
	public void statementShouldExecuteQueryAndExposeResultSet() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.rows(Arrays.asList("name"),
				Collections.<Map<String, Object>>singletonList(row("name", "Neo"))));
		Statement statement = GraphJdbcStatementSupport.statement(null, executor, identity(),
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		ResultSet rs = statement.executeQuery("RETURN name");
		assertEquals("RETURN name", executor.sql);
		assertTrue(rs.next());
		assertEquals("Neo", rs.getString("name"));
		assertSame(rs, statement.getResultSet());
		assertEquals(-1, statement.getUpdateCount());
	}

	@Test
	public void preparedStatementShouldBindParametersAndRejectStringExecute() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.updateCount(3));
		PreparedStatement statement = GraphJdbcStatementSupport.preparedStatement(null, executor, identity(),
				"UPDATE n SET name = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
				ResultSet.CLOSE_CURSORS_AT_COMMIT);
		statement.setString(1, "Trinity");
		assertEquals(3, statement.executeUpdate());
		assertEquals("Trinity", executor.bindings.get(1));
		try {
			statement.executeQuery("SELECT 1");
			fail("PreparedStatement executeQuery(String) must fail");
		}
		catch (SQLException expected) {
			assertTrue(expected.getMessage().contains("cannot be called on PreparedStatement"));
		}
		try {
			statement.addBatch("UPDATE n SET name = 'Morpheus'");
			fail("PreparedStatement addBatch(String) must fail");
		}
		catch (SQLException expected) {
			assertTrue(expected.getMessage().contains("cannot be called on PreparedStatement"));
		}
	}

	@Test
	public void batchShouldReturnUpdateCountsAndClearBatch() throws Exception {
		RecordingExecutor executor = new RecordingExecutor(GraphJdbcResult.updateCount(1));
		Statement statement = GraphJdbcStatementSupport.statement(null, executor, identity(),
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		statement.addBatch("A");
		statement.addBatch("B");
		assertArrayEquals(new int[] {1, 1}, statement.executeBatch());
		assertArrayEquals(new int[0], statement.executeBatch());
	}

	private static GraphJdbcSqlPreprocessor identity() {
		return new GraphJdbcSqlPreprocessor() {
			@Override
			public String preprocess(String sql) {
				return sql;
			}
		};
	}

	private static Map<String, Object> row(String name, Object value) {
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put(name, value);
		return row;
	}

	private static final class RecordingExecutor implements GraphJdbcExecutor {

		private final GraphJdbcResult result;
		private String sql;
		private GraphJdbcParameterBindings bindings;

		private RecordingExecutor(GraphJdbcResult result) {
			this.result = result;
		}

		@Override
		public GraphJdbcResult execute(String sql, GraphJdbcParameterBindings bindings) {
			this.sql = sql;
			this.bindings = bindings;
			return result;
		}
	}
}
