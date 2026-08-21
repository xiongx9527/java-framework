package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeRecord;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeValue;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaParameterBindings;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import org.junit.After;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.xiongx9527.fw.plus.graph.nebula.jdbc.test.NativeResultSets.rows;

public class NebulaStandardJdbcIntegrationTest {

	@After
	public void tearDown() {
		NebulaDriver.resetConnectionFactory();
	}

	@Test
	public void driverManagerPreparedStatementShouldReadRowsByColumnLabel() throws Exception {
		Class.forName(NebulaDriver.class.getName());
		final RecordingExecutor executor = new RecordingExecutor(resultSetWithSingleString("playerName", "Tim"));
		NebulaDriver.setConnectionFactory(new NebulaDriver.ConnectionFactory() {
			@Override
			public Connection connect(String url, Properties properties) throws SQLException {
				final NebulaConnection connection = mock(NebulaConnection.class);
				when(connection.isClosed()).thenReturn(false);
				when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
					String sql = invocation.getArgument(0);
					return new NebulaPreparedStatementImpl(connection, sql, executor);
				});
				return connection;
			}
		});

		Connection connection = DriverManager.getConnection("jdbc:nebula://127.0.0.1:9669/test_space", new Properties());
		PreparedStatement statement = connection.prepareStatement("SELECT name AS playerName FROM player WHERE id = ?");
		statement.setLong(1, 1001L);
		ResultSet resultSet = statement.executeQuery();

		assertTrue(resultSet.next());
		assertEquals("Tim", resultSet.getString("playerName"));
		assertEquals("Tim", resultSet.getObject(1));
		assertFalse(resultSet.wasNull());
		assertEquals(1, resultSet.findColumn("playerName"));
		assertEquals("playerName", resultSet.getMetaData().getColumnLabel(1));
		assertFalse(resultSet.next());
		assertEquals("SELECT name AS playerName FROM player WHERE id = ?", executor.lastSql);
		assertEquals(Long.valueOf(1001L), executor.lastBindings.get(1).getValue());
	}

	private static ResultSet resultSetWithSingleString(String columnName, String value) throws Exception {
		return new NebulaResultSet(rows(Collections.singletonList(columnName), new String[][] {{value}}), null);
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet;
		private String lastSql;
		private NebulaParameterBindings lastBindings;

		private RecordingExecutor(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			this.lastSql = sql;
			this.lastBindings = bindings;
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			throw new SQLException("not used");
		}
	}
}
