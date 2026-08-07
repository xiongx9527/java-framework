package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ManagedMagicDynamicDataSourceTest {

	@Test
	void putAndGetReturnsItsExactNodeBeforeAnEquivalentOrdinaryPutCanReplaceIt() throws Exception {
		ManagedMagicDynamicDataSource dynamicDataSource = new ManagedMagicDynamicDataSource();
		BlockingCloseDataSource oldDataSource = new BlockingCloseDataSource();
		DataSource sharedDataSource = mock(DataSource.class);
		dynamicDataSource.put("old-id", "orders", "Orders", oldDataSource, 100);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		AtomicReference<Thread> replacementThread = new AtomicReference<Thread>();
		try {
			Future<MagicDynamicDataSource.DataSourceNode> published = executor.submit(() ->
					dynamicDataSource.putAndGet(
							"same-id", "orders", "Orders", sharedDataSource, 100));
			assertThat(oldDataSource.closeEntered.await(5, TimeUnit.SECONDS)).isTrue();
			Future<?> replacement = executor.submit(() -> {
				replacementThread.set(Thread.currentThread());
				dynamicDataSource.put(
						"same-id", "orders", "Orders", sharedDataSource, 100);
			});

			assertThat(awaitBlocked(replacementThread)).isEqualTo(Thread.State.BLOCKED);
			oldDataSource.releaseClose.countDown();
			MagicDynamicDataSource.DataSourceNode publishedNode = published.get(5, TimeUnit.SECONDS);
			replacement.get(5, TimeUnit.SECONDS);
			MagicDynamicDataSource.DataSourceNode currentNode =
					dynamicDataSource.getDataSource("orders");

			assertThat(publishedNode).isNotSameAs(currentNode);
			assertThat(publishedNode.getId()).isEqualTo("same-id");
			assertThat(publishedNode.getKey()).isEqualTo("orders");
			assertThat(publishedNode.getName()).isEqualTo("Orders");
			assertThat(publishedNode.getDataSource()).isSameAs(sharedDataSource);
			assertThat(currentNode.getDataSource()).isSameAs(sharedDataSource);
		} finally {
			oldDataSource.releaseClose.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void collectionReadsReturnStableSnapshotsThatCannotMutateTheRegistry() {
		ManagedMagicDynamicDataSource dynamicDataSource = new ManagedMagicDynamicDataSource();
		DataSource first = mock(DataSource.class);
		DataSource second = mock(DataSource.class);
		dynamicDataSource.put("first", first);

		Collection<MagicDynamicDataSource.DataSourceNode> nodeSnapshot =
				dynamicDataSource.datasourceNodes();
		List<String> keySnapshot = dynamicDataSource.datasources();
		dynamicDataSource.put("second", second);

		assertThat(nodeSnapshot).extracting(MagicDynamicDataSource.DataSourceNode::getKey)
				.containsExactly("first");
		assertThat(keySnapshot).containsExactly("first");
		nodeSnapshot.clear();
		keySnapshot.clear();
		assertThat(dynamicDataSource.datasources()).containsExactlyInAnyOrder("first", "second");
	}

	private static Thread.State awaitBlocked(AtomicReference<Thread> threadReference) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		Thread thread = threadReference.get();
		while (thread == null || thread.getState() != Thread.State.BLOCKED) {
			if (System.nanoTime() >= deadline) {
				throw new AssertionError("replacement worker did not block; state="
						+ (thread == null ? null : thread.getState()));
			}
			Thread.yield();
			thread = threadReference.get();
		}
		return thread.getState();
	}

	private static final class BlockingCloseDataSource implements DataSource, Closeable {

		private final CountDownLatch closeEntered = new CountDownLatch(1);
		private final CountDownLatch releaseClose = new CountDownLatch(1);

		@Override
		public void close() throws IOException {
			closeEntered.countDown();
			try {
				if (!releaseClose.await(5, TimeUnit.SECONDS)) {
					throw new IOException("close release timed out");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("interrupted", e);
			}
		}

		@Override
		public Connection getConnection() throws SQLException {
			throw new SQLException("not used");
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			throw new SQLException("not used");
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
