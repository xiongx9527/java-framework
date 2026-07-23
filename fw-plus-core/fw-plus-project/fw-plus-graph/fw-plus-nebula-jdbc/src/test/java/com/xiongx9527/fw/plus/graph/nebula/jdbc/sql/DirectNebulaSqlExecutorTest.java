package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.BinlogEventBatch;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.BinlogEventStore;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.BinlogPosition;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.InMemoryBinlogEventStore;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.NebulaBinlogEventDraft;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.NebulaBinlogEventType;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.TableMapping;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger.TriggerAwareNebulaSqlExecutor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaWriteConversionException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaWriteErrorCode;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

public class DirectNebulaSqlExecutorTest {

	@Test
	public void executeQueryShouldThrowWhenNativeResultFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = failedNativeResult();
		when(connection.execute("YIELD 1")).thenReturn(nativeResultSet);

		assertNativeFailure(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DirectNebulaSqlExecutor().executeQuery(
						connection,
						"YIELD 1",
						NebulaParameterBindings.empty()
				);
			}
		});
	}

	@Test
	public void executeUpdateShouldThrowWhenNativeResultFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = failedNativeResult();
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
		NebulaNativeResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);
		NebulaParameterBindings bindings = new NebulaParameterBindings(3);
		bindings.bind(1, 1001L, Types.BIGINT);
		bindings.bind(2, true, Types.BOOLEAN);
		bindings.bind(3, "Tim", Types.VARCHAR);

		new DirectNebulaSqlExecutor().executeQuery(
				connection,
				"YIELD '?' AS marker, ? AS id, ? AS active, ? AS name",
				bindings
		);

		verify(connection).execute("YIELD '?' AS marker, 1001 AS id, true AS active, \"Tim\" AS name");
	}

	@Test
	public void executeQueryShouldRouteMappedSqlSelectThroughQueryConverter() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		new DirectNebulaSqlExecutor(personMappings()).executeQuery(
				connection,
				"SELECT name FROM person WHERE id = 'p1' LIMIT 10",
				NebulaParameterBindings.empty()
		);

		verify(connection).execute("MATCH (person:person) WHERE id(person) == \"p1\" RETURN person.name AS name LIMIT 10");
	}

	@Test
	public void executeQueryShouldNotFallbackToRawSqlWhenMappedSqlSelectNativeExecutionFails() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = failedNativeResult();
		when(connection.execute("MATCH (person:person) RETURN person.name AS name")).thenReturn(nativeResultSet);

		try {
			new DirectNebulaSqlExecutor(personMappings()).executeQuery(
					connection,
					"SELECT name FROM person",
					NebulaParameterBindings.empty()
			);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("MATCH (person:person) RETURN person.name AS name"));
		}

		verify(connection, never()).execute("SELECT name FROM person");
	}

	@Test
	public void executeUpdateShouldRouteSqlInsertAndDdlThroughConverters() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
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
		NebulaNativeResultSet nativeResultSet = successNativeResult();
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
		assertEquals("DROP TABLE person", batch.getEvents().get(1).getSql());
	}

	@Test
	public void executeUpdateShouldNotAppendBinlogEventForMetadataStatements() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");

		try {
			new DirectNebulaSqlExecutor(personMappings(), store).executeUpdate(
					connection,
					"DESCRIBE person",
					NebulaParameterBindings.empty()
			);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("Update nGql must start with an update keyword"));
		}

		assertTrue(store.readAfter(null, 10).isEmpty());
	}

	@Test
	public void executeUpdateShouldNotAppendBinlogEventForNativeNgqlFallback() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
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
		NebulaNativeResultSet nativeResultSet = failedNativeResult();
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
		NebulaNativeResultSet nativeResultSet = successNativeResult();
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
	public void executeUpdateShouldCreateTriggerMetadataAndFireBodySqlOnGatewayWrite() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
		when(connection.getSchema()).thenReturn("test_space");
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		DirectNebulaSqlExecutor executor = new DirectNebulaSqlExecutor(triggerMappings());
		int ddlRows = executor.executeUpdate(
				connection,
				"CREATE TRIGGER person_ai AFTER INSERT ON person FOR EACH ROW "
						+ "INSERT INTO audit(id, marker) VALUES ('a1', 'seen;still')",
				NebulaParameterBindings.empty()
		);
		int insertRows = executor.executeUpdate(
				connection,
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
				NebulaParameterBindings.empty()
		);

		assertEquals(0, ddlRows);
		assertEquals(1, insertRows);
		verify(connection, never()).execute("CREATE TRIGGER person_ai AFTER INSERT ON person FOR EACH ROW "
				+ "INSERT INTO audit(id, marker) VALUES ('a1', 'seen;still')");
		InOrder order = inOrder(connection);
		order.verify(connection).execute("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")");
		order.verify(connection).execute("INSERT VERTEX audit(marker) VALUES \"a1\":(\"seen;still\")");
	}

	@Test
	public void executeUpdateShouldKeepNativeNgqlFallbackOutsideTriggerPath() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
		when(connection.getSchema()).thenReturn("test_space");
		when(connection.execute(anyString())).thenReturn(nativeResultSet);

		DirectNebulaSqlExecutor executor = new DirectNebulaSqlExecutor(triggerMappings());
		executor.executeUpdate(
				connection,
				"CREATE TRIGGER person_ai AFTER INSERT ON person FOR EACH ROW "
						+ "INSERT INTO audit(id, marker) VALUES ('a1', 'seen')",
				NebulaParameterBindings.empty()
		);
		executor.executeUpdate(
				connection,
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")",
				NebulaParameterBindings.empty()
		);

		verify(connection).execute("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")");
		verify(connection, never()).execute("INSERT VERTEX audit(marker) VALUES \"a1\":(\"seen\")");
	}

	@Test
	public void defaultExecutorProviderShouldUseTriggerAwareSqlExecutor() {
		assertTrue(NebulaSqlExecutorProvider.getExecutor() instanceof TriggerAwareNebulaSqlExecutor);
	}

	@Test
	public void executeUpdateShouldKeepNativeNgqlFallback() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaNativeResultSet nativeResultSet = successNativeResult();
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

	private NebulaNativeResultSet failedNativeResult() throws SQLException {
		NebulaNativeResultSet nativeResultSet = mock(NebulaNativeResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(false);
		when(nativeResultSet.getErrorCode()).thenReturn("INVALID_SYNTAX");
		when(nativeResultSet.getErrorMessage()).thenReturn("syntax error");
		return nativeResultSet;
	}

	private NebulaNativeResultSet successNativeResult() throws SQLException {
		NebulaNativeResultSet nativeResultSet = mock(NebulaNativeResultSet.class);
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

	private NebulaSchemaMappingRegistry triggerMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.addTable(TableMapping.tag("audit", "audit")
						.idColumn("id", Types.VARCHAR)
						.property("marker", "marker", Types.VARCHAR)
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
			assertTrue(exception.getMessage().contains("INVALID_SYNTAX"));
			assertTrue(exception.getMessage().contains("syntax error"));
		}
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
