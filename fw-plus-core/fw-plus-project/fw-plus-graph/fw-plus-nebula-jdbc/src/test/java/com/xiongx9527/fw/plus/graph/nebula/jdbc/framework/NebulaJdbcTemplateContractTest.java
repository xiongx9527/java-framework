package com.xiongx9527.fw.plus.graph.nebula.jdbc.framework;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaJdbcTemplateContractTest {

	@After
	public void tearDown() {
		NebulaSqlExecutorProvider.reset();
	}

	@Test
	public void jdbcTemplateQueryForObjectShouldBindArgumentsAndReadScalar() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mockOpenConnection();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

		String value = jdbcTemplate.queryForObject(
				"SELECT name FROM player WHERE id = ?",
				new Object[]{1001L},
				String.class
		);

		assertEquals("Tim", value);
		assertEquals("SELECT name FROM player WHERE id = ?", executor.getLastSql());
		assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
	}

	@Test
	public void jdbcTemplateRowMapperShouldReadColumnsByLabel() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singlePlayer("Tim", 18);
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mockOpenConnection();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));

		List<PlayerRecord> rows = jdbcTemplate.query(
				"SELECT name AS playerName, age FROM player WHERE age > ?",
				new Object[]{10},
				new RowMapper<PlayerRecord>() {
					@Override
					public PlayerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
						PlayerRecord record = new PlayerRecord();
						record.setPlayerName(rs.getString("playerName"));
						record.setAge(Integer.valueOf(rs.getInt("age")));
						return record;
					}
				}
		);

		assertEquals(1, rows.size());
		assertEquals("Tim", rows.get(0).getPlayerName());
		assertEquals(Integer.valueOf(18), rows.get(0).getAge());
		assertEquals(Integer.valueOf(10), executor.getLastBindings().get(1).getValue());
	}

	private NebulaConnection mockOpenConnection() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.isClosed()).thenReturn(false);
		when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
			String sql = invocation.getArgument(0);
			PreparedStatement statement = new NebulaPreparedStatementImpl(connection, sql);
			return statement;
		});
		return connection;
	}
}
