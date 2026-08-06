package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.MagicDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyDataSourceMagicDynamicRegistryDecoratorTest {

	@Test
	void appliesDecoratorsInOrderAndNotifiesBeforeReplacingOldNode() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		CloseTrackingDataSource replacement = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldDataSource, 100);
		List<String> events = new ArrayList<String>();
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new RecordingDecorator("first", events));
		decorators.add(new RecordingDecorator("second", events));
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(replacement), decorators);

		assertThat(registry.registerDirect(info("new-id", "orders"))).isTrue();

		assertThat(events).containsExactly("decorate:first", "decorate:second",
				"remove:first:orders", "remove:second:orders");
		assertThat(dynamicDataSource.getDataSource("orders").getDataSource()).isSameAs(replacement);
		assertThat(oldDataSource.closeCount).isOne();
	}

	@Test
	void buildFailureKeepsOldNodeAndReturnsFalse() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldDataSource, 100);
		List<String> events = new ArrayList<String>();
		MagicResourceStorage<DataSourceInfo> storage = mock(MagicResourceStorage.class);
		when(storage.buildKey(any(DataSourceInfo.class))).thenReturn("orders-resource");
		Map<String, DataSourceBuilder> builders = new HashMap<String, DataSourceBuilder>();
		builders.put("test", new FailingBuilder());
		MyDataSourceMagicDynamicRegistry registry = new MyDataSourceMagicDynamicRegistry(
				storage, dynamicDataSource, builders,
				Collections.<MagicDataSourceDecorator>singletonList(new RecordingDecorator("only", events)));

		assertThat(registry.register(info("new-id", "orders"))).isFalse();

		assertThat(dynamicDataSource.getDataSource("orders").getDataSource()).isSameAs(oldDataSource);
		assertThat(oldDataSource.closeCount).isZero();
		assertThat(events).isEmpty();
	}

	@Test
	void notifiesDecoratorsBeforeDeletingNode() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldDataSource, 100);
		List<String> events = new ArrayList<String>();
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(new CloseTrackingDataSource()),
				Collections.<MagicDataSourceDecorator>singletonList(new RecordingDecorator("only", events)));

		registry.unregisterDirect("orders");

		assertThat(events).containsExactly("remove:only:orders");
		assertThat(dynamicDataSource.datasources()).doesNotContain("orders");
		assertThat(oldDataSource.closeCount).isOne();
	}

	@Test
	void notifiesOldKeyWhenSameIdMovesToANewKey() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		CloseTrackingDataSource replacement = new CloseTrackingDataSource();
		dynamicDataSource.put("same-id", "old-orders", "Old", oldDataSource, 100);
		List<String> events = new ArrayList<String>();
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(replacement),
				Collections.<MagicDataSourceDecorator>singletonList(new RecordingDecorator("only", events)));

		assertThat(registry.registerDirect(info("same-id", "new-orders"))).isTrue();

		assertThat(events).containsExactly("decorate:only", "remove:only:old-orders");
		assertThat(dynamicDataSource.datasources()).contains("new-orders").doesNotContain("old-orders");
		assertThat(oldDataSource.closeCount).isOne();
	}

	@Test
	void replacementVetoRunsAllHooksKeepsOldWrapperOpenAndClosesNewChain() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldRaw = new CloseTrackingDataSource();
		DbTxAwareDataSource oldAware = new DbTxAwareDataSource("orders", oldRaw,
				Collections.<DbTxDataSourceAdapter>emptyList());
		CloseTrackingDataSource replacement = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldAware, 100);
		List<String> events = new ArrayList<String>();
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList()));
		decorators.add(new VetoDecorator("first", events));
		decorators.add(new VetoDecorator("second", events));
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(replacement), decorators);

		assertThatThrownBy(() -> registry.registerDirect(info("new-id", "orders")))
				.isInstanceOf(IllegalStateException.class);

		assertThat(events).containsExactly("remove:first:orders", "remove:second:orders");
		assertThat(dynamicDataSource.getDataSource("orders").getDataSource()).isSameAs(oldAware);
		assertThat(oldRaw.closeCount).isZero();
		assertThat(replacement.closeCount).isOne();
	}

	@Test
	void deleteVetoRunsAllHooksAndKeepsNodeOpen() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldDataSource, 100);
		List<String> events = new ArrayList<String>();
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new VetoDecorator("first", events));
		decorators.add(new VetoDecorator("second", events));
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(new CloseTrackingDataSource()), decorators);

		assertThatThrownBy(() -> registry.unregisterDirect("orders"))
				.isInstanceOf(IllegalStateException.class);

		assertThat(events).containsExactly("remove:first:orders", "remove:second:orders");
		assertThat(dynamicDataSource.getDataSource("orders").getDataSource()).isSameAs(oldDataSource);
		assertThat(oldDataSource.closeCount).isZero();
	}

	@Test
	void decoratorFailureClosesNewChainAndKeepsOldNode() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldDataSource = new CloseTrackingDataSource();
		CloseTrackingDataSource replacement = new CloseTrackingDataSource();
		dynamicDataSource.put("old-id", "orders", "Old", oldDataSource, 100);
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList()));
		decorators.add(new ThrowingDecorator());
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(replacement), decorators);

		assertThatThrownBy(() -> registry.registerDirect(info("new-id", "orders")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("decorate failed");

		assertThat(dynamicDataSource.getDataSource("orders").getDataSource()).isSameAs(oldDataSource);
		assertThat(oldDataSource.closeCount).isZero();
		assertThat(replacement.closeCount).isOne();
	}

	@Test
	void decoratorFailureRollsBackEverySuccessfulStageAndClosesRawOnce() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource raw = new CloseTrackingDataSource();
		List<String> cleanupEvents = new ArrayList<String>();
		OwnedStageDecorator first = new OwnedStageDecorator("first", cleanupEvents);
		OwnedStageDecorator second = new OwnedStageDecorator("second", cleanupEvents);
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(first);
		decorators.add(second);
		decorators.add(new ThrowingDecorator());
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(raw), decorators);

		assertThatThrownBy(() -> registry.registerDirect(info("new-id", "orders")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("decorate failed");

		assertThat(first.stage.closeCount).isOne();
		assertThat(second.stage.closeCount).isOne();
		assertThat(raw.closeCount).isOne();
		assertThat(cleanupEvents).containsExactly("second", "first");
	}

	@Test
	void cleanupFailureIsSuppressedAndRemainingStagesAndRawAreClosed() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource raw = new CloseTrackingDataSource();
		OwnedStageDecorator first = new OwnedStageDecorator();
		OwnedStageDecorator second = new OwnedStageDecorator(new IOException("cleanup failed"));
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(first);
		decorators.add(second);
		decorators.add(new ThrowingDecorator());
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(raw), decorators);

		assertThatThrownBy(() -> registry.registerDirect(info("new-id", "orders")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("decorate failed")
				.satisfies(failure -> assertThat(failure.getSuppressed())
						.extracting(Throwable::getMessage)
						.containsExactly("cleanup failed"));

		assertThat(first.stage.closeCount).isOne();
		assertThat(second.stage.closeCount).isZero();
		assertThat(raw.closeCount).isOne();
	}

	private static TestRegistry registry(MagicDynamicDataSource dynamicDataSource,
			DataSourceBuilder builder, List<MagicDataSourceDecorator> decorators) {
		MagicResourceStorage<DataSourceInfo> storage = mock(MagicResourceStorage.class);
		Map<String, DataSourceBuilder> builders = new HashMap<String, DataSourceBuilder>();
		builders.put("test", builder);
		return new TestRegistry(storage, dynamicDataSource, builders, decorators);
	}

	private static DataSourceInfo info(String id, String key) {
		DataSourceInfo info = new DataSourceInfo();
		info.setId(id);
		info.setKey(key);
		info.setName("Orders");
		info.setType("test");
		info.setUrl("jdbc:h2:mem:" + key);
		info.setDriverClassName("org.h2.Driver");
		info.setMaxRows(100);
		return info;
	}

	private static final class TestRegistry extends MyDataSourceMagicDynamicRegistry {

		private TestRegistry(MagicResourceStorage<DataSourceInfo> storage,
				MagicDynamicDataSource dynamicDataSource,
				Map<String, DataSourceBuilder> builders,
				List<MagicDataSourceDecorator> decorators) {
			super(storage, dynamicDataSource, builders, decorators);
		}

		private boolean registerDirect(DataSourceInfo info) {
			MappingNode<DataSourceInfo> node = new MappingNode<DataSourceInfo>(info);
			node.setMappingKey(info.getKey());
			return register(node);
		}

		private void unregisterDirect(String key) {
			DataSourceInfo info = info("old-id", key);
			MappingNode<DataSourceInfo> node = new MappingNode<DataSourceInfo>(info);
			node.setMappingKey(key);
			unregister(node);
		}
	}

	private static final class FixedBuilder extends DataSourceBuilder {

		private final DataSource dataSource;

		private FixedBuilder(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		DataSource buildDataSource(String type, Map<String, Object> properties) {
			return dataSource;
		}
	}

	private static final class FailingBuilder extends DataSourceBuilder {

		@Override
		DataSource buildDataSource(String type, Map<String, Object> properties) {
			throw new IllegalStateException("build failed");
		}
	}

	private static final class RecordingDecorator implements MagicDataSourceDecorator {

		private final String name;
		private final List<String> events;

		private RecordingDecorator(String name, List<String> events) {
			this.name = name;
			this.events = events;
		}

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			events.add("decorate:" + name);
			return dataSource;
		}

		@Override
		public void beforeRemove(String key, DataSource dataSource) {
			events.add("remove:" + name + ":" + key);
		}
	}

	private static final class VetoDecorator implements MagicDataSourceDecorator {

		private final String name;
		private final List<String> events;

		private VetoDecorator(String name, List<String> events) {
			this.name = name;
			this.events = events;
		}

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			return dataSource;
		}

		@Override
		public void beforeRemove(String key, DataSource dataSource) {
			events.add("remove:" + name + ":" + key);
			throw new IllegalStateException("veto:" + name);
		}
	}

	private static final class ThrowingDecorator implements MagicDataSourceDecorator {

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			throw new IllegalStateException("decorate failed");
		}
	}

	private static final class OwnedStageDecorator implements MagicDataSourceDecorator {

		private final CloseTrackingDataSource stage = new CloseTrackingDataSource();
		private final String name;
		private final List<String> cleanupEvents;
		private final Exception cleanupFailure;

		private OwnedStageDecorator() {
			this(null, null, null);
		}

		private OwnedStageDecorator(Exception cleanupFailure) {
			this(null, null, cleanupFailure);
		}

		private OwnedStageDecorator(String name, List<String> cleanupEvents) {
			this(name, cleanupEvents, null);
		}

		private OwnedStageDecorator(String name, List<String> cleanupEvents, Exception cleanupFailure) {
			this.name = name;
			this.cleanupEvents = cleanupEvents;
			this.cleanupFailure = cleanupFailure;
		}

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			return stage;
		}

		@Override
		public void cleanupAfterDecorationFailure(String key, DataSource dataSource) throws Exception {
			if (cleanupEvents != null) {
				cleanupEvents.add(name);
			}
			if (cleanupFailure != null) {
				throw cleanupFailure;
			}
			((AutoCloseable) dataSource).close();
		}
	}

	private static final class CloseTrackingDataSource implements DataSource, Closeable {

		private int closeCount;

		@Override
		public Connection getConnection() throws SQLException {
			throw new SQLException("not used");
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			throw new SQLException("not used");
		}

		@Override
		public void close() throws IOException {
			closeCount++;
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
