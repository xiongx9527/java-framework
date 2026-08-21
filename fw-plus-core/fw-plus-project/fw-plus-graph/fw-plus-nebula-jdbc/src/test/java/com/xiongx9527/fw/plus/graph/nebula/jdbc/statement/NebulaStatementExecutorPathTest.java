package com.xiongx9527.fw.plus.graph.nebula.jdbc.statement;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaParameterBindings;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.BinlogEventBatch;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.InMemoryBinlogEventStore;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog.NebulaBinlogEventType;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.TableMapping;
import org.junit.After;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaStatementExecutorPathTest {

	@After
	public void tearDown() {
		NebulaSqlExecutorProvider.reset();
	}

	@Test
	public void executeQueryShouldDelegateSqlAndEmptyBindingsToExecutor() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		ResultSet resultSet = statement.executeQuery("SELECT name FROM player");

		assertSame(executor.resultSet, resultSet);
		assertEquals("SELECT name FROM player", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	@Test
	public void executeShouldDelegateQuerySqlAndEmptyBindingsToExecutor() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		boolean hasResultSet = statement.execute("SELECT name FROM player");

		assertTrue(hasResultSet);
		assertSame(executor.resultSet, statement.getResultSet());
		assertSame(executor.resultSet, statement.getResultSet());
		assertEquals("SELECT name FROM player", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	@Test
	public void executeShouldDelegateUpdateSqlAndReturnFalse() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaStatementImpl statement = new NebulaStatementImpl(connection);

		boolean hasResultSet = statement.execute("INSERT VERTEX player(name) VALUES 1:(\"Tim\")");

		assertEquals(false, hasResultSet);
		assertSame(null, statement.getResultSet());
		assertEquals("INSERT VERTEX player(name) VALUES 1:(\"Tim\")", executor.sql);
		assertEquals(0, executor.bindings.size());
		assertSame(connection, executor.connection);
	}

	@Test
	public void resultSetCapabilityGettersShouldReturnSupportedConstantsWithoutCurrentResultSet() throws Exception {
		NebulaStatementImpl statement = new NebulaStatementImpl(mock(NebulaConnection.class));

		assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, statement.getResultSetHoldability());
		assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency());
		assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, statement.getResultSetType());
	}

	@Test
	public void providerOverrideShouldNotLeakAcrossThreads() throws Exception {
		RecordingExecutor executor = new RecordingExecutor();
		NebulaSqlExecutorProvider.setExecutor(executor);
		final AtomicReference<NebulaSqlExecutor> executorFromOtherThread = new AtomicReference<NebulaSqlExecutor>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				executorFromOtherThread.set(NebulaSqlExecutorProvider.getExecutor());
			}
		});

		thread.start();
		thread.join();

		assertSame(executor, NebulaSqlExecutorProvider.getExecutor());
		assertNotSame(executor, executorFromOtherThread.get());
	}

	@Test
	public void defaultStatementPathShouldUseConfiguredBinlogEventStore() throws Exception {
		InMemoryBinlogEventStore store = new InMemoryBinlogEventStore("store-a");
		NebulaSqlExecutorProvider.configureDefaultExecutor(personMappings(), store);
		NebulaConnection connection = mock(NebulaConnection.class);
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet nativeResultSet =
				mock(com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		when(connection.execute("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")")).thenReturn(nativeResultSet);
		Statement statement = new NebulaStatementImpl(connection);

		statement.executeUpdate("INSERT INTO person(id, name) VALUES ('p1', 'Tom')");

		BinlogEventBatch batch = store.readAfter(null, 10);
		assertEquals(1, batch.getEvents().size());
		assertEquals(NebulaBinlogEventType.WRITE_ROWS, batch.getEvents().get(0).getEventType());
	}

	@Test
	public void defaultStatementPathShouldUseConfiguredSchemaMappingWithoutBinlogStore() throws Exception {
		NebulaSqlExecutorProvider.configureDefaultExecutor(personMappings());
		NebulaConnection connection = mock(NebulaConnection.class);
		com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet nativeResultSet =
				mock(com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		when(connection.execute("MATCH (person:person) RETURN person.name AS name")).thenReturn(nativeResultSet);
		Statement statement = new NebulaStatementImpl(connection);

		statement.executeQuery("SELECT name FROM person");

		org.mockito.Mockito.verify(connection).execute("MATCH (person:person) RETURN person.name AS name");
	}

	private NebulaSchemaMappingRegistry personMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet = mock(ResultSet.class);
		private NebulaConnection connection;
		private String sql;
		private NebulaParameterBindings bindings;

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			this.connection = connection;
			this.sql = sql;
			this.bindings = bindings;
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			this.connection = connection;
			this.sql = sql;
			this.bindings = bindings;
			return 1;
		}
	}
}
