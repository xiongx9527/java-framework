package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventBatch;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventStore;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogPosition;
import com.vesoft.nebula.jdbc.sql.binlog.InMemoryBinlogEventStore;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventDraft;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventType;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteConversionException;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteErrorCode;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

	@Test
	public void executeQueryShouldRenderPreparedStatementBindingsBeforeNativeExecution() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);
		NebulaParameterBindings bindings = new NebulaParameterBindings(3);
		bindings.bind(1, 1001L, Types.BIGINT);
		bindings.bind(2, true, Types.BOOLEAN);
		bindings.bind(3, "Tim", Types.VARCHAR);

		new DirectNebulaSqlExecutor().executeQuery(
				connection,
				"SELECT '?' AS marker FROM player WHERE id = ? AND active = ? AND name = ?",
				bindings
		);

		verify(connection).execute("SELECT '?' AS marker FROM player WHERE id = 1001 AND active = true AND name = \"Tim\"");
	}

	@Test
	public void executeUpdateShouldRouteSqlInsertAndDdlThroughConverters() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		DirectNebulaSqlExecutor executor = new DirectNebulaSqlExecutor(personMappings());
		int insertRows = executor.executeUpdate(
				connection,
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
				NebulaParameterBindings.empty()
		);
		int ddlRows = executor.executeUpdate(
				connection,
				"DROP TABLE person",
				NebulaParameterBindings.empty()
		);

		assertTrue(insertRows == 1);
		assertTrue(ddlRows == 0);
		verify(connection).execute("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")");
		verify(connection).execute("DROP TAG person");
	}

	@Test
	public void executeUpdateShouldAppendBinlogEventsForSqlInsertAndDdl() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");

		DirectNebulaSqlExecutor executor = new DirectNebulaSqlExecutor(personMappings(), store);
		executor.executeUpdate(
				connection,
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
				NebulaParameterBindings.empty()
		);
		executor.executeUpdate(
				connection,
				"DROP TABLE person",
				NebulaParameterBindings.empty()
		);

		BinlogEventBatch batch = store.readAfter(null, 10);
		assertEquals(2, batch.getEvents().size());
		assertEquals(NebulaBinlogEventType.WRITE_ROWS, batch.getEvents().get(0).getEventType());
		assertEquals("person", batch.getEvents().get(0).getSchemaName());
		assertEquals("INSERT_VERTEX", batch.getEvents().get(0).getPayload().get("mutationKind"));
		assertEquals(NebulaBinlogEventType.SCHEMA_CHANGE, batch.getEvents().get(1).getEventType());
		assertEquals("DROP_TAG", batch.getEvents().get(1).getPayload().get("ddlKind"));
	}

	@Test
	public void executeUpdateShouldNotAppendBinlogEventForNativeNgqlFallback() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");

		new DirectNebulaSqlExecutor(personMappings(), store).executeUpdate(
				connection,
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")",
				NebulaParameterBindings.empty()
		);

		assertTrue(store.readAfter(null, 10).isEmpty());
	}

	@Test
	public void executeUpdateShouldNotAppendBinlogEventWhenNativeExecutionFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = failedNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");

		try {
			new DirectNebulaSqlExecutor(personMappings(), store).executeUpdate(
					connection,
					"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
					NebulaParameterBindings.empty()
			);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("executed failed"));
		}

		assertTrue(store.readAfter(null, 10).isEmpty());
	}

	@Test
	public void executeUpdateShouldFailWhenBinlogAppendFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		try {
			new DirectNebulaSqlExecutor(personMappings(), failingStore()).executeUpdate(
					connection,
					"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
					NebulaParameterBindings.empty()
			);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("binlog-compatible stream append failed"));
		}
	}

	@Test
	public void executeUpdateShouldKeepNativeNgqlFallback() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		new DirectNebulaSqlExecutor(personMappings()).executeUpdate(
				connection,
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")",
				NebulaParameterBindings.empty()
		);

		verify(connection).execute("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")");
	}

	@Test
	public void executeUpdateShouldRejectTransactionsExplicitly() throws Exception {
		try {
			new DirectNebulaSqlExecutor(personMappings()).executeUpdate(
					mock(NebulaConnection.class),
					"BEGIN",
					NebulaParameterBindings.empty()
			);
			fail("Expected NebulaWriteConversionException");
		} catch (NebulaWriteConversionException exception) {
			assertTrue(exception.getMessage().contains(NebulaWriteErrorCode.NEBULA_SQL_TRANSACTION_UNSUPPORTED.name()));
		}
	}

	@Test
	public void executeUpdateShouldRejectComplexSqlDdlWithoutNativeFallback() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		try {
			new DirectNebulaSqlExecutor(personMappings()).executeUpdate(
					connection,
					"ALTER TABLE person ADD INDEX idx_name(name)",
					NebulaParameterBindings.empty()
			);
			fail("Expected NebulaWriteConversionException");
		} catch (NebulaWriteConversionException exception) {
			assertTrue(exception.getMessage().contains(NebulaWriteErrorCode.NEBULA_SQL_DDL_UNSUPPORTED_OPTION.name()));
		}
	}

	private ResultSet failedNativeResult() {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(false);
		when(nativeResultSet.getErrorCode()).thenReturn(-1004);
		when(nativeResultSet.getErrorMessage()).thenReturn("syntax error");
		return nativeResultSet;
	}

	private ResultSet successNativeResult() {
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		return nativeResultSet;
	}

	private NebulaSchemaMappingRegistry personMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
	}

	private BinlogEventStore failingStore() {
		return new BinlogEventStore() {
			@Override
			public BinlogPosition append(NebulaBinlogEventDraft event) throws SQLException {
				throw new SQLException("store unavailable");
			}

			@Override
			public BinlogEventBatch readAfter(BinlogPosition position, int maxEvents) throws SQLException {
				return new BinlogEventBatch(null, null);
			}

			@Override
			public BinlogPosition currentPosition() throws SQLException {
				return new BinlogPosition(0L, "failing-store");
			}
		};
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
