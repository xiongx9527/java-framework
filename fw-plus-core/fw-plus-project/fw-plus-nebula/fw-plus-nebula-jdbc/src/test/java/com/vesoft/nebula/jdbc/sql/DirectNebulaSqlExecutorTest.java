package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DirectNebulaSqlExecutorTest {

	@Test
	public void executeQueryShouldThrowWhenNativeResultFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = failedNativeResult();
		when(connection.execute("SELECT 1")).thenReturn(nativeResultSet);

		assertNativeFailure(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DirectNebulaSqlExecutor().executeQuery(
						connection,
						"SELECT 1",
						NebulaParameterBindings.empty()
				);
			}
		});
	}

	@Test
	public void executeUpdateShouldThrowWhenNativeResultFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = failedNativeResult();
		when(connection.execute("INSERT VERTEX player(name) VALUES 1:(\"Tim\")")).thenReturn(nativeResultSet);

		assertNativeFailure(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DirectNebulaSqlExecutor().executeUpdate(
						connection,
						"INSERT VERTEX player(name) VALUES 1:(\"Tim\")",
						NebulaParameterBindings.empty()
				);
			}
		});
	}

	private ResultSet failedNativeResult() {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(false);
		when(nativeResultSet.getErrorCode()).thenReturn(-1004);
		when(nativeResultSet.getErrorMessage()).thenReturn("syntax error");
		return nativeResultSet;
	}

	private void assertNativeFailure(SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("executed failed"));
			assertTrue(exception.getMessage().contains("-1004"));
			assertTrue(exception.getMessage().contains("syntax error"));
		}
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
