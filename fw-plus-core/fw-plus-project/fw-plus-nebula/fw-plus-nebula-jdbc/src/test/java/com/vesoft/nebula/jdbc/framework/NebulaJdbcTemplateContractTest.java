package com.vesoft.nebula.jdbc.framework;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
