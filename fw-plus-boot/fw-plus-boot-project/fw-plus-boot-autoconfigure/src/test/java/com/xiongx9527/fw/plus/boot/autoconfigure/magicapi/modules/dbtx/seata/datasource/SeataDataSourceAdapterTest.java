package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.SeataDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator.SeataTransaction;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
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
	void validatesTheTargetDatabaseBeforeReusingAnExistingSeataProxy() throws Exception {
		MetadataDataSource target = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy existing = mock(SeataDataSourceProxy.class);
		when(existing.getTargetDataSource()).thenReturn(target);
		AtomicInteger proxyCreations = new AtomicInteger();
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			proxyCreations.incrementAndGet();
			return mock(SeataDataSourceProxy.class);
		});
		DbTxAwareDataSource aware = aware("orders", existing, adapter);

		assertThat(aware.prepare("seata")).isSameAs(existing);
		assertThat(target.connectionCount.get()).isOne();
		assertThat(proxyCreations.get()).isZero();
	}

	@Test
	void rejectsAnExistingUnsupportedProxyBeforeTransactionLookupBeginOrCallback() {
		DataSourceProxy existing = mock(DataSourceProxy.class);
		when(existing.getDbType()).thenReturn("h2");
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			throw new AssertionError("must not create a second proxy");
		});
		DbTxAwareDataSource aware = aware("orders", existing, adapter);
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		dynamicDataSource.put("orders", aware);
		AtomicBoolean transactionLookup = new AtomicBoolean();
		AtomicBoolean globalBegin = new AtomicBoolean();
		AtomicBoolean callback = new AtomicBoolean();
		SeataDbTxCoordinator coordinator = new SeataDbTxCoordinator(dynamicDataSource, () -> {
			transactionLookup.set(true);
			return new NoOpLauncher(globalBegin);
		});

		Throwable thrown = catchThrowable(() -> coordinator.execute(
				new DbTxDefinition("seata", Collections.singletonList("orders"), 60000, "test"),
				() -> {
					callback.set(true);
					return "unused";
				}));

		assertThat(thrown).isInstanceOf(DbTxException.class);
		assertThat(((DbTxException) thrown).getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_BEGIN_FAILED);
		assertThat(thrown).hasMessageContaining("orders");
		assertThat(transactionLookup).isFalse();
		assertThat(globalBegin).isFalse();
		assertThat(callback).isFalse();
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

	@Test
	void closesThePhysicalTargetOnceWhenUnusedWrappersReuseAnExistingSeataProxy() throws Exception {
		MetadataDataSource target = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy existing = mock(SeataDataSourceProxy.class);
		when(existing.getTargetDataSource()).thenReturn(target);
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			throw new AssertionError("must reuse the existing proxy");
		});
		DbTxAwareDataSource first = aware("orders", existing, adapter);
		DbTxAwareDataSource second = aware("orders-copy", existing, adapter);

		first.close();
		second.close();

		assertThat(target.closeCount.get()).isOne();
	}

	@Test
	void doesNotCloseThePhysicalTargetOfAPreparedExistingSeataProxy() throws Exception {
		MetadataDataSource target = new MetadataDataSource("jdbc:mysql://localhost:3306/orders");
		SeataDataSourceProxy existing = mock(SeataDataSourceProxy.class);
		when(existing.getTargetDataSource()).thenReturn(target);
		SeataDataSourceAdapter adapter = new SeataDataSourceAdapter(dataSource -> {
			throw new AssertionError("must reuse the existing proxy");
		});
		DbTxAwareDataSource aware = aware("orders", existing, adapter);
		aware.prepare("seata");

		aware.close();

		assertThat(target.closeCount.get()).isZero();
	}

	private static DbTxAwareDataSource aware(String key, DataSource raw,
			DbTxDataSourceAdapter adapter) {
		return new DbTxAwareDataSource(key, raw,
				Collections.<DbTxDataSourceAdapter>singletonList(adapter));
	}

	private static final class NoOpLauncher implements SeataTransaction {

		private final AtomicBoolean globalBegin;

		private NoOpLauncher(AtomicBoolean globalBegin) {
			this.globalBegin = globalBegin;
		}

		@Override
		public boolean isLauncher() {
			return true;
		}

		@Override
		public void begin(int timeoutMs, String name) {
			globalBegin.set(true);
		}

		@Override
		public void commit() {
		}

		@Override
		public void rollback() {
		}

		@Override
		public String getXid() {
			return "unused";
		}

		@Override
		public GlobalStatus getGlobalStatus() {
			return GlobalStatus.Begin;
		}
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
