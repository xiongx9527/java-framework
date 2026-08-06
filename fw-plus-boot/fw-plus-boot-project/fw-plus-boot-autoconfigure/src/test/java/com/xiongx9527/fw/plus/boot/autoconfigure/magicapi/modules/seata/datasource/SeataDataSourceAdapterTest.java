package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import io.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
import static org.mockito.Mockito.when;

class SeataDataSourceAdapterTest {

	@Test
	void createsOneProxyOnlyWhenTheParticipatingWrapperIsPreparedConcurrently() throws Exception {
		MetadataDataSource raw = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);
		AtomicInteger proxyCreations = new AtomicInteger();
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			proxyCreations.incrementAndGet();
			return proxy;
		});
		DbTxAwareDataSource aware = aware("orders", raw, adapter);

		assertThat(raw.connectionCount.get()).isZero();
		assertThat(proxyCreations.get()).isZero();

		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Callable<DataSource>> tasks = java.util.Arrays.asList(
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"),
					() -> aware.prepare("seata"), () -> aware.prepare("seata"));
			for (Future<DataSource> future : executor.invokeAll(tasks)) {
				assertThat(future.get()).isSameAs(proxy);
			}
		} finally {
			executor.shutdownNow();
		}

		assertThat(raw.connectionCount.get()).isOne();
		assertThat(proxyCreations.get()).isOne();
	}

	@Test
	void reusesAnExistingSeataProxyWithoutInspectingOrProxyingItAgain() throws Exception {
		SeataDataSourceProxy existing = mock(SeataDataSourceProxy.class);
		AtomicInteger proxyCreations = new AtomicInteger();
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			proxyCreations.incrementAndGet();
			return mock(SeataDataSourceProxy.class);
		});
		DbTxAwareDataSource aware = aware("orders", existing, adapter);

		assertThat(aware.prepare("seata")).isSameAs(existing);
		assertThat(proxyCreations.get()).isZero();
	}

	@Test
	void rejectsAnUnsupportedJdbcDatabaseBeforeCreatingASeataProxy() {
		MetadataDataSource raw = new MetadataDataSource("jdbc:h2:mem:orders");
		AtomicInteger proxyCreations = new AtomicInteger();
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			proxyCreations.incrementAndGet();
			return mock(SeataDataSourceProxy.class);
		});
		DbTxAwareDataSource aware = aware("orders", raw, adapter);

		assertThatThrownBy(() -> aware.prepare("seata"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("orders")
				.hasMessageContaining("h2");
		assertThat(proxyCreations.get()).isZero();
		assertThat(raw.connectionCount.get()).isOne();
	}

	@Test
	void rejectsHotRemovalAfterASeataProxyHasBeenPrepared() throws Exception {
		MetadataDataSource raw = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> proxy);
		DbTxAwareDataSource aware = aware("orders", raw, adapter);
		aware.prepare("seata");
		DbTxDataSourceDecorator decorator = new DbTxDataSourceDecorator(
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));

		assertThatThrownBy(() -> decorator.beforeRemove("orders", aware))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("orders")
				.hasRootCauseMessage("A prepared Seata DataSource cannot be removed before restart");
		assertThat(raw.closeCount.get()).isZero();
	}

	@Test
	void leavesPhysicalPoolClosingToTheDbTxWrapper() throws Exception {
		MetadataDataSource raw = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> proxy);
		DbTxAwareDataSource aware = aware("orders", raw, adapter);
		aware.prepare("seata");

		adapter.close(proxy);
		assertThat(raw.closeCount.get()).isZero();

		aware.close();
		assertThat(raw.closeCount.get()).isOne();
	}

	private static DbTxAwareDataSource aware(String key, DataSource raw,
			DbTxDataSourceAdapter adapter) {
		return new DbTxAwareDataSource(key, raw,
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));
	}

	private static final class MetadataDataSource implements DataSource, AutoCloseable {

		private final String jdbcUrl;
		private final AtomicInteger connectionCount = new AtomicInteger();
		private final AtomicInteger closeCount = new AtomicInteger();

		private MetadataDataSource(String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
		}

		@Override
		public Connection getConnection() throws SQLException {
			connectionCount.incrementAndGet();
			DatabaseMetaData metadata = mock(DatabaseMetaData.class);
			when(metadata.getURL()).thenReturn(jdbcUrl);
			Connection connection = mock(Connection.class);
			when(connection.getMetaData()).thenReturn(metadata);
			return connection;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return getConnection();
		}

		@Override
		public void close() {
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
