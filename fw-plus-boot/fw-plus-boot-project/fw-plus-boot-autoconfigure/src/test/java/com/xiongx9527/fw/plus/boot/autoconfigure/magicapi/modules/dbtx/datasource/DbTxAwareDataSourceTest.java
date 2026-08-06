package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DbTxAwareDataSourceTest {

	@AfterEach
	void clearContext() {
		DbTxContextHolder.clear();
	}

	@Test
	void usesRawDataSourceWithoutContextAndForLocalCoordinator() throws Exception {
		Connection rawConnection = mock(Connection.class);
		CountingDataSource raw = new CountingDataSource(rawConnection);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw, Collections.<DbTxDataSourceAdapter>emptyList());

		assertThat(aware.getConnection()).isSameAs(rawConnection);
		DbTxContextHolder.open(definition("local", "a"));
		assertThat(aware.getConnection()).isSameAs(rawConnection);
		assertThat(raw.connectionCount.get()).isEqualTo(2);
	}

	@Test
	void rejectsUndeclaredKeyBeforeAccessingRawDataSource() {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		DbTxAwareDataSource aware = new DbTxAwareDataSource("b", raw, Collections.<DbTxDataSourceAdapter>emptyList());
		DbTxContextHolder.open(definition("local", "a"));

		assertThatThrownBy(aware::getConnection)
				.isInstanceOf(SQLException.class)
				.hasMessageContaining("b");
		assertThat(raw.connectionCount.get()).isZero();
	}

	@Test
	void preparesAdapterOnceAndRoutesConnectionsThroughPreparedDelegate() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		Connection adaptedConnection = mock(Connection.class);
		CountingDataSource adapted = new CountingDataSource(adaptedConnection);
		RecordingAdapter adapter = new RecordingAdapter("seata", adapted);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw, Collections.<DbTxDataSourceAdapter>singletonList(adapter));

		assertThat(aware.prepare("seata")).isSameAs(adapted);
		assertThat(aware.prepare("seata")).isSameAs(adapted);
		DbTxContextHolder.open(definition("seata", "a"));
		assertThat(aware.getConnection()).isSameAs(adaptedConnection);
		assertThat(adapter.prepareCount.get()).isOne();
		assertThat(raw.connectionCount.get()).isZero();
	}

	@Test
	void preparesAdapterOnlyOnceWhenCalledConcurrently() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		CountingDataSource adapted = new CountingDataSource(mock(Connection.class));
		RecordingAdapter adapter = new RecordingAdapter("seata", adapted);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw, Collections.<DbTxDataSourceAdapter>singletonList(adapter));
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Callable<DataSource>> tasks = Arrays.asList(
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"));
			List<Future<DataSource>> futures = executor.invokeAll(tasks);
			for (Future<DataSource> future : futures) {
				assertThat(future.get()).isSameAs(adapted);
			}
		} finally {
			executor.shutdownNow();
		}
		assertThat(adapter.prepareCount.get()).isOne();
	}

	@Test
	void decoratorAvoidsDuplicateWrapping() {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		DbTxDataSourceDecorator decorator = new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList());

		DataSource first = decorator.decorate("a", raw);
		DataSource second = decorator.decorate("a", first);

		assertThat(first).isInstanceOf(DbTxAwareDataSource.class);
		assertThat(second).isSameAs(first);
	}

	@Test
	void closesPreparedDelegateAndRawPoolExactlyOnce() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		CountingDataSource adapted = new CountingDataSource(mock(Connection.class));
		RecordingAdapter adapter = new RecordingAdapter("seata", adapted);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw, Collections.<DbTxDataSourceAdapter>singletonList(adapter));
		aware.prepare("seata");

		aware.close();
		aware.close();

		assertThat(adapter.closeCount.get()).isOne();
		assertThat(adapter.closedDataSource).isSameAs(adapted);
		assertThat(raw.closeCount.get()).isOne();
	}

	@Test
	void invokesAdapterCloseWhenPreparedDelegateIsRawWithoutDoubleClosingRaw() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		RecordingAdapter adapter = new RecordingAdapter("seata", raw);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw,
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));
		aware.prepare("seata");

		aware.close();

		assertThat(adapter.closeCount.get()).isOne();
		assertThat(adapter.closedDataSource).isSameAs(raw);
		assertThat(raw.closeCount.get()).isOne();
	}

	@Test
	void closeProcessesEveryAdapterAndRawOnceWhenOneAdapterFails() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		RecordingAdapter failing = new RecordingAdapter("seata", new CountingDataSource(mock(Connection.class)));
		failing.closeFailure = new IllegalStateException("seata close failed");
		RecordingAdapter succeeding = new RecordingAdapter("other", new CountingDataSource(mock(Connection.class)));
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw, Arrays.asList(failing, succeeding));
		aware.prepare("seata");
		aware.prepare("other");

		assertThatThrownBy(aware::close)
				.isInstanceOf(IOException.class)
				.hasMessageContaining("a");
		aware.close();

		assertThat(failing.closeCount.get()).isOne();
		assertThat(succeeding.closeCount.get()).isOne();
		assertThat(raw.closeCount.get()).isOne();
	}

	@Test
	void beforeRemoveValidatesWithoutClosingWrapperOrRawPool() throws Exception {
		CountingDataSource raw = new CountingDataSource(mock(Connection.class));
		RecordingAdapter adapter = new RecordingAdapter("seata", raw);
		DbTxAwareDataSource aware = new DbTxAwareDataSource("a", raw,
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));
		aware.prepare("seata");
		DbTxDataSourceDecorator decorator = new DbTxDataSourceDecorator(
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));

		decorator.beforeRemove("a", aware);

		assertThat(adapter.beforeRemoveCount.get()).isOne();
		assertThat(adapter.closeCount.get()).isZero();
		assertThat(raw.closeCount.get()).isZero();
		assertThat(aware.getConnection()).isSameAs(raw.connection);
	}

	private static DbTxDefinition definition(String coordinator, String... keys) {
		return new DbTxDefinition(coordinator, Arrays.asList(keys), 60000, "test");
	}

	private static final class RecordingAdapter implements DbTxDataSourceAdapter {

		private final String coordinator;
		private final DataSource prepared;
		private final AtomicInteger prepareCount = new AtomicInteger();
		private final AtomicInteger beforeRemoveCount = new AtomicInteger();
		private final AtomicInteger closeCount = new AtomicInteger();
		private volatile DataSource closedDataSource;
		private volatile RuntimeException closeFailure;

		private RecordingAdapter(String coordinator, DataSource prepared) {
			this.coordinator = coordinator;
			this.prepared = prepared;
		}

		@Override
		public String getCoordinator() {
			return coordinator;
		}

		@Override
		public DataSource prepare(String key, DataSource raw) {
			prepareCount.incrementAndGet();
			return prepared;
		}

		public void beforeRemove(DataSource dataSource) {
			beforeRemoveCount.incrementAndGet();
		}

		@Override
		public void close(DataSource dataSource) {
			closedDataSource = dataSource;
			closeCount.incrementAndGet();
			if (closeFailure != null) {
				throw closeFailure;
			}
		}
	}

	private static final class CountingDataSource implements DataSource, Closeable {

		private final Connection connection;
		private final AtomicInteger connectionCount = new AtomicInteger();
		private final AtomicInteger closeCount = new AtomicInteger();

		private CountingDataSource(Connection connection) {
			this.connection = connection;
		}

		@Override
		public Connection getConnection() {
			connectionCount.incrementAndGet();
			return connection;
		}

		@Override
		public Connection getConnection(String username, String password) {
			return getConnection();
		}

		@Override
		public void close() throws IOException {
			closeCount.incrementAndGet();
		}

		@Override
		public PrintWriter getLogWriter() {
			return null;
		}

		@Override
		public void setLogWriter(PrintWriter out) {
		}

		@Override
		public void setLoginTimeout(int seconds) {
		}

		@Override
		public int getLoginTimeout() {
			return 0;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			throw new SQLException("not a wrapper");
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			return false;
		}
	}
}
