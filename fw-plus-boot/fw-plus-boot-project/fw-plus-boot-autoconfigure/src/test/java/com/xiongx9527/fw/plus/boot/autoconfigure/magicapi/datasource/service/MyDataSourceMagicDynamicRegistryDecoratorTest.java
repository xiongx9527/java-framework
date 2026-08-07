package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.MagicDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.LocalDbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceDecorator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
	void replacementVetoRunsAllHooksKeepsOldWrapperOpenAndClosesNewChain() throws Exception {
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
		DbTxAwareDataSource.Lease lease = oldAware.acquireLease();
		lease.close();
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

	@Test
	void activeLocalTransactionVetoesReplacementAndCommitsThroughOriginalDataSource() throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingH2DataSource oldRaw = h2DataSource("replace-old");
		CloseTrackingH2DataSource replacementRaw = h2DataSource("replace-new");
		JdbcTemplate oldJdbc = new JdbcTemplate(oldRaw);
		JdbcTemplate replacementJdbc = new JdbcTemplate(replacementRaw);
		oldJdbc.execute("create table tx_test(id integer primary key)");
		replacementJdbc.execute("create table tx_test(id integer primary key)");
		DbTxAwareDataSource oldAware = new DbTxAwareDataSource("orders", oldRaw,
				Collections.<DbTxDataSourceAdapter>emptyList());
		dynamicDataSource.put("old-id", "orders", "Old", oldAware, 100);
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(replacementRaw),
				Collections.<MagicDataSourceDecorator>singletonList(
						new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList())));
		LocalDbTxCoordinator coordinator = new LocalDbTxCoordinator(dynamicDataSource);
		CountDownLatch callbackStarted = new CountDownLatch(1);
		CountDownLatch releaseCallback = new CountDownLatch(1);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Object> outcome = executor.submit(() -> {
			try {
				return coordinator.execute(localDefinition(), () -> {
					callbackStarted.countDown();
					if (!releaseCallback.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("callback release timed out");
					}
					new JdbcTemplate(oldAware).update("insert into tx_test(id) values (1)");
					return "committed";
				});
			} catch (Throwable failure) {
				return failure;
			}
		});

		assertThat(callbackStarted.await(5, TimeUnit.SECONDS)).isTrue();
		Throwable replacementFailure = org.assertj.core.api.Assertions.catchThrowable(
				() -> registry.registerDirect(info("new-id", "orders")));
		int oldCloseCountWhileActive = oldRaw.closeCount;
		DataSource nodeWhileActive = dynamicDataSource.getDataSource("orders").getDataSource();
		releaseCallback.countDown();
		Object transactionOutcome;
		try {
			transactionOutcome = outcome.get(5, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		assertThat(replacementFailure).isInstanceOf(IllegalStateException.class);
		assertThat(oldCloseCountWhileActive).isZero();
		assertThat(nodeWhileActive).isSameAs(oldAware);
		assertThat(transactionOutcome).isEqualTo("committed");
		assertThat(count(oldJdbc)).isOne();
		assertThat(count(replacementJdbc)).isZero();
		assertThat(replacementRaw.closeCount).isOne();

		registry.unregisterDirect("orders");
		assertThat(oldRaw.closeCount).isOne();
	}

	@Test
	void activeLocalTransactionVetoesDeletionAndRollsBackThroughOriginalDataSource() throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingH2DataSource oldRaw = h2DataSource("delete-old");
		JdbcTemplate oldJdbc = new JdbcTemplate(oldRaw);
		oldJdbc.execute("create table tx_test(id integer primary key)");
		DbTxAwareDataSource oldAware = new DbTxAwareDataSource("orders", oldRaw,
				Collections.<DbTxDataSourceAdapter>emptyList());
		dynamicDataSource.put("old-id", "orders", "Old", oldAware, 100);
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(h2DataSource("unused")),
				Collections.<MagicDataSourceDecorator>singletonList(
						new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList())));
		LocalDbTxCoordinator coordinator = new LocalDbTxCoordinator(dynamicDataSource);
		CountDownLatch callbackStarted = new CountDownLatch(1);
		CountDownLatch releaseCallback = new CountDownLatch(1);
		IllegalStateException callbackFailure = new IllegalStateException("rollback");
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Throwable> outcome = executor.submit(() -> {
			try {
				coordinator.execute(localDefinition(), () -> {
					callbackStarted.countDown();
					if (!releaseCallback.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("callback release timed out");
					}
					new JdbcTemplate(oldAware).update("insert into tx_test(id) values (1)");
					throw callbackFailure;
				});
				return null;
			} catch (Throwable failure) {
				return failure;
			}
		});

		assertThat(callbackStarted.await(5, TimeUnit.SECONDS)).isTrue();
		Throwable deleteFailure = org.assertj.core.api.Assertions.catchThrowable(
				() -> registry.unregisterDirect("orders"));
		int oldCloseCountWhileActive = oldRaw.closeCount;
		DataSource nodeWhileActive = findDataSource(dynamicDataSource, "orders");
		releaseCallback.countDown();
		Throwable transactionFailure;
		try {
			transactionFailure = outcome.get(5, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		assertThat(deleteFailure).isInstanceOf(IllegalStateException.class);
		assertThat(oldCloseCountWhileActive).isZero();
		assertThat(nodeWhileActive).isSameAs(oldAware);
		assertThat(transactionFailure).isSameAs(callbackFailure);
		assertThat(count(oldJdbc)).isZero();

		registry.unregisterDirect("orders");
		assertThat(oldRaw.closeCount).isOne();
	}

	@Test
	void replacementPassesTheDbTxDecorationStageWhenAnOuterDecoratorWrapsIt() throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldRaw = new CloseTrackingDataSource();
		CloseTrackingDataSource replacementRaw = new CloseTrackingDataSource();
		PreparedVetoAdapter adapter = new PreparedVetoAdapter();
		OuterDataSourceDecorator outer = new OuterDataSourceDecorator();
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new DbTxDataSourceDecorator(
				Collections.<DbTxDataSourceAdapter>singletonList(adapter)));
		decorators.add(outer);
		TestRegistry registry = registry(dynamicDataSource,
				new SequenceBuilder(oldRaw, replacementRaw), decorators);
		assertThat(registry.registerDirect(info("old-id", "orders"))).isTrue();
		DataSource oldNode = findDataSource(dynamicDataSource, "orders");
		outer.dbTxStages.get(0).prepare("prepared");

		Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
				() -> registry.registerDirect(info("new-id", "orders")));

		assertThat(failure).isInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("prepared stage veto");
		assertThat(findDataSource(dynamicDataSource, "orders")).isSameAs(oldNode);
		assertThat(oldRaw.closeCount).isZero();
		assertThat(replacementRaw.closeCount).isOne();
	}

	@Test
	void deletionPassesTheDbTxDecorationStageWhenAnOuterDecoratorWrapsIt() throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		CloseTrackingDataSource oldRaw = new CloseTrackingDataSource();
		PreparedVetoAdapter adapter = new PreparedVetoAdapter();
		OuterDataSourceDecorator outer = new OuterDataSourceDecorator();
		List<MagicDataSourceDecorator> decorators = new ArrayList<MagicDataSourceDecorator>();
		decorators.add(new DbTxDataSourceDecorator(
				Collections.<DbTxDataSourceAdapter>singletonList(adapter)));
		decorators.add(outer);
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(oldRaw), decorators);
		assertThat(registry.registerDirect(info("old-id", "orders"))).isTrue();
		DataSource oldNode = findDataSource(dynamicDataSource, "orders");
		outer.dbTxStages.get(0).prepare("prepared");

		Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
				() -> registry.unregisterDirect("orders"));

		assertThat(failure).isInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("prepared stage veto");
		assertThat(findDataSource(dynamicDataSource, "orders")).isSameAs(oldNode);
		assertThat(oldRaw.closeCount).isZero();
	}

	@Test
	void releasesEarlierLeasesWhenALaterParticipantCannotBeAcquired() throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		DbTxAwareDataSource first = new DbTxAwareDataSource("first", new CloseTrackingDataSource(),
				Collections.<DbTxDataSourceAdapter>emptyList());
		DbTxAwareDataSource second = new DbTxAwareDataSource("second", new CloseTrackingDataSource(),
				Collections.<DbTxDataSourceAdapter>emptyList());
		dynamicDataSource.put("first", first);
		dynamicDataSource.put("second", second);
		second.startRemove();
		LocalDbTxCoordinator coordinator = new LocalDbTxCoordinator(dynamicDataSource);

		Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> coordinator.execute(
				new DbTxDefinition("local", Arrays.asList("first", "second"),
						60000, "partial-lease"),
				() -> {
					throw new AssertionError("callback must not run");
				}));

		assertThat(failure).isInstanceOf(DbTxException.class)
				.hasMessageContaining("second");
		first.beforeRemove();
		first.cancelRemove();
		second.cancelRemove();
	}

	@Test
	void failedConcurrentRemovalDoesNotCancelAnotherRegistrysClaim() throws Exception {
		BlockingDeleteMagicDynamicDataSource dynamicDataSource = new BlockingDeleteMagicDynamicDataSource();
		DbTxAwareDataSource oldAware = new DbTxAwareDataSource("orders", new CloseTrackingDataSource(),
				Collections.<DbTxDataSourceAdapter>emptyList());
		dynamicDataSource.put("old-id", "orders", "Old", oldAware, 100);
		List<MagicDataSourceDecorator> decorators = Collections.<MagicDataSourceDecorator>singletonList(
				new DbTxDataSourceDecorator(Collections.<DbTxDataSourceAdapter>emptyList()));
		TestRegistry firstRegistry = registry(dynamicDataSource,
				new FixedBuilder(new CloseTrackingDataSource()), decorators);
		TestRegistry secondRegistry = registry(dynamicDataSource,
				new FixedBuilder(new CloseTrackingDataSource()), decorators);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Throwable> firstRemoval = executor.submit(() ->
					org.assertj.core.api.Assertions.catchThrowable(
							() -> firstRegistry.unregisterDirect("orders")));
			assertThat(dynamicDataSource.deleteEntered.await(5, TimeUnit.SECONDS)).isTrue();
			Future<Throwable> secondRemoval = executor.submit(() ->
					org.assertj.core.api.Assertions.catchThrowable(
							() -> secondRegistry.unregisterDirect("orders")));

			assertThat(secondRemoval.get(5, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
			Throwable leaseFailure = org.assertj.core.api.Assertions.catchThrowable(() -> {
				try (DbTxAwareDataSource.Lease ignored = oldAware.acquireLease()) {
				}
			});

			assertThat(leaseFailure).isInstanceOf(SQLException.class)
					.hasMessageContaining("being removed");
			dynamicDataSource.releaseDelete.countDown();
			assertThat(firstRemoval.get(5, TimeUnit.SECONDS)).isNull();
		} finally {
			dynamicDataSource.releaseDelete.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void registrationPublishesNodeAndDecorationStagesAtomically() throws Exception {
		BlockingPublicationMagicDynamicDataSource dynamicDataSource =
				new BlockingPublicationMagicDynamicDataSource();
		PerRegistrationStageDecorator stageDecorator = new PerRegistrationStageDecorator();
		CloseTrackingDataSource finalDataSource = new CloseTrackingDataSource();
		List<MagicDataSourceDecorator> decorators = Arrays.<MagicDataSourceDecorator>asList(
				stageDecorator, new SharedFinalDataSourceDecorator(finalDataSource));
		TestRegistry registry = registry(dynamicDataSource,
				new FixedBuilder(new CloseTrackingDataSource()), decorators);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> registration = executor.submit(() ->
					registry.registerDirect(info("orders-id", "orders")));
			assertThat(dynamicDataSource.nodePublished.await(5, TimeUnit.SECONDS)).isTrue();
			CountDownLatch unregisterAttempting = new CountDownLatch(1);
			AtomicReference<Thread> removalThread = new AtomicReference<Thread>();
			Future<Throwable> removal = executor.submit(() -> {
				removalThread.set(Thread.currentThread());
				return org.assertj.core.api.Assertions.catchThrowable(
						() -> registry.unregisterDirect("orders", unregisterAttempting));
			});

			assertThat(unregisterAttempting.await(5, TimeUnit.SECONDS)).isTrue();
			Thread.State removalState = awaitBlockedOrDeleteEntered(
					removalThread.get(), dynamicDataSource.deleteEntered);
			assertThat(removalState)
					.as("removal worker state when deleteEntered=%s",
							dynamicDataSource.deleteEntered.getCount())
					.isEqualTo(Thread.State.BLOCKED);
			assertThat(dynamicDataSource.deleteEntered.getCount())
					.as("removal worker state: %s", removalState)
					.isOne();
			dynamicDataSource.releasePut.countDown();
			assertThat(registration.get(5, TimeUnit.SECONDS)).isTrue();
			assertThat(dynamicDataSource.deleteEntered.await(5, TimeUnit.SECONDS)).isTrue();
			dynamicDataSource.releaseDelete.countDown();
			assertThat(removal.get(5, TimeUnit.SECONDS)).isNull();
			assertThat(stageDecorator.removedStages.get("orders"))
					.isSameAs(stageDecorator.createdStages.get(0));
		} finally {
			dynamicDataSource.releasePut.countDown();
			dynamicDataSource.releaseDelete.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void sharedFinalDataSourceKeepsDecorationStagesOwnedByEachNode() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		PerRegistrationStageDecorator stageDecorator = new PerRegistrationStageDecorator();
		CloseTrackingDataSource sharedFinalDataSource = new CloseTrackingDataSource();
		List<MagicDataSourceDecorator> decorators = Arrays.<MagicDataSourceDecorator>asList(
				stageDecorator, new SharedFinalDataSourceDecorator(sharedFinalDataSource));
		TestRegistry registry = registry(dynamicDataSource,
				new SequenceBuilder(new CloseTrackingDataSource(), new CloseTrackingDataSource()), decorators);

		assertThat(registry.registerDirect(info("first-id", "first"))).isTrue();
		assertThat(registry.registerDirect(info("second-id", "second"))).isTrue();
		DataSource firstStage = stageDecorator.createdStages.get(0);
		DataSource secondStage = stageDecorator.createdStages.get(1);

		registry.unregisterDirect("first");
		registry.unregisterDirect("second");

		assertThat(stageDecorator.removedStages.get("first")).isSameAs(firstStage);
		assertThat(stageDecorator.removedStages.get("second")).isSameAs(secondStage);
	}

	@Test
	void equivalentReplacementAfterPublicationDoesNotReceiveThePublishedNodesStages() {
		EquivalentReplacementManagedMagicDynamicDataSource dynamicDataSource =
				new EquivalentReplacementManagedMagicDynamicDataSource();
		PerRegistrationStageDecorator stageDecorator = new PerRegistrationStageDecorator();
		CloseTrackingDataSource sharedFinalDataSource = new CloseTrackingDataSource();
		TestRegistry registry = registry(dynamicDataSource,
				new FixedBuilder(new CloseTrackingDataSource()),
				Arrays.<MagicDataSourceDecorator>asList(
						stageDecorator, new SharedFinalDataSourceDecorator(sharedFinalDataSource)));

		assertThat(registry.registerDirect(info("orders-id", "orders"))).isTrue();
		DataSource publishedStage = stageDecorator.createdStages.get(0);
		registry.unregisterDirect("orders");

		assertThat(stageDecorator.removedStages.get("orders"))
				.isSameAs(sharedFinalDataSource)
				.isNotSameAs(publishedStage);
	}

	@Test
	void customParentSubtypeFailsFastInsteadOfInferringAnEquivalentReplacement() {
		EquivalentReplacementMagicDynamicDataSource dynamicDataSource =
				new EquivalentReplacementMagicDynamicDataSource();
		CloseTrackingDataSource rawDataSource = new CloseTrackingDataSource();
		OwnedStageDecorator decorator = new OwnedStageDecorator();
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(rawDataSource),
				Collections.<MagicDataSourceDecorator>singletonList(decorator));

		assertThatThrownBy(() -> registry.registerDirect(info("orders-id", "orders")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ManagedMagicDynamicDataSource");
		assertThat(rawDataSource.closeCount).isOne();
		assertThat(decorator.stage.closeCount).isOne();
	}

	@Test
	void hiddenPublishedNodeSnapshotDoesNotPreventExactNodeRegistration() {
		HidingPublishedNodeMagicDynamicDataSource dynamicDataSource =
				new HidingPublishedNodeMagicDynamicDataSource();
		AvailableDataSource rawDataSource = new AvailableDataSource();
		CascadingStageDecorator first = new CascadingStageDecorator();
		CascadingStageDecorator second = new CascadingStageDecorator();
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(rawDataSource),
				Arrays.<MagicDataSourceDecorator>asList(first, second));

		assertThat(registry.registerDirect(info("orders-id", "orders"))).isTrue();
		assertThat(rawDataSource.closeCount).isZero();
		assertThat(first.stages.get(0).closeCount).isZero();
		assertThat(second.stages.get(0).closeCount).isZero();
		assertThat(dynamicDataSource.datasourceNodes()).isEmpty();

		registry.unregisterDirect("orders");

		assertThat(first.removedStages.get("orders")).isSameAs(first.stages.get(0));
		assertThat(second.removedStages.get("orders")).isSameAs(second.stages.get(0));
		assertThat(rawDataSource.closeCount).isOne();
		assertThat(first.stages.get(0).closeCount).isOne();
		assertThat(second.stages.get(0).closeCount).isOne();
		assertThat(first.cleanupCount).isZero();
		assertThat(second.cleanupCount).isZero();
	}

	@Test
	void managedPublicationBlocksAnotherRegistryUntilTheNodeIsReady() throws Exception {
		BlockingPublicationMagicDynamicDataSource dynamicDataSource =
				new BlockingPublicationMagicDynamicDataSource();
		AvailableDataSource firstRaw = new AvailableDataSource();
		CascadingStageDecorator firstInner = new CascadingStageDecorator();
		CascadingStageDecorator firstOuter = new CascadingStageDecorator();
		TestRegistry firstRegistry = registry(dynamicDataSource, new FixedBuilder(firstRaw),
				Arrays.<MagicDataSourceDecorator>asList(firstInner, firstOuter));
		AvailableDataSource replacementRaw = new AvailableDataSource();
		CascadingStageDecorator replacementDecorator = new CascadingStageDecorator();
		TestRegistry replacementRegistry = registry(dynamicDataSource, new FixedBuilder(replacementRaw),
				Collections.<MagicDataSourceDecorator>singletonList(replacementDecorator));
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> firstRegistration = executor.submit(() ->
					firstRegistry.registerDirect(info("first-id", "orders")));
			assertThat(dynamicDataSource.nodePublished.await(5, TimeUnit.SECONDS)).isTrue();
			AtomicReference<Thread> replacementThread = new AtomicReference<Thread>();
			CountDownLatch replacementAttempting = new CountDownLatch(1);
			Future<Boolean> replacementRegistration = executor.submit(() -> {
				replacementThread.set(Thread.currentThread());
				replacementAttempting.countDown();
				return replacementRegistry.registerDirect(info("replacement-id", "orders"));
			});
			assertThat(replacementAttempting.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(awaitBlockedOrDeleteEntered(
					replacementThread.get(), dynamicDataSource.deleteEntered))
					.isEqualTo(Thread.State.BLOCKED);

			dynamicDataSource.releasePut.countDown();
			assertThat(firstRegistration.get(5, TimeUnit.SECONDS)).isTrue();
			assertThat(replacementRegistration.get(5, TimeUnit.SECONDS)).isTrue();

			DataSource replacement = findDataSource(dynamicDataSource, "orders");
			assertThat(replacement).isSameAs(replacementDecorator.stages.get(0));
			assertThat(replacement.getConnection()).isSameAs(replacementRaw.connection);
			assertThat(replacementRaw.closeCount).isZero();
			assertThat(replacementDecorator.stages.get(0).closeCount).isZero();
			assertThat(firstRaw.closeCount).isOne();
			assertThat(firstInner.stages.get(0).closeCount).isOne();
			assertThat(firstOuter.stages.get(0).closeCount).isOne();
			assertThat(firstInner.cleanupCount).isZero();
			assertThat(firstOuter.cleanupCount).isZero();
			assertThat(replacementDecorator.removedStages.get("orders"))
					.isSameAs(firstOuter.stages.get(0));

			dynamicDataSource.releaseDelete.countDown();
			replacementRegistry.unregisterDirect("orders");
			assertThat(replacementDecorator.removedStages.get("orders"))
					.isSameAs(replacementDecorator.stages.get(0));
		} finally {
			dynamicDataSource.releasePut.countDown();
			dynamicDataSource.releaseDelete.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void exactParentCompatibilityPublishesAndRemovesItsDecorationStages() {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		AvailableDataSource rawDataSource = new AvailableDataSource();
		CascadingStageDecorator decorator = new CascadingStageDecorator();
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(rawDataSource),
				Collections.<MagicDataSourceDecorator>singletonList(decorator));

		assertThat(registry.registerDirect(info("orders-id", "orders"))).isTrue();
		registry.unregisterDirect("orders");

		assertThat(decorator.removedStages.get("orders")).isSameAs(decorator.stages.get(0));
		assertThat(rawDataSource.closeCount).isOne();
		assertThat(decorator.stages.get(0).closeCount).isOne();
		assertThat(decorator.cleanupCount).isZero();
	}

	@Test
	void startRemoveErrorRollsBackEarlierClaims() throws Exception {
		assertRemovalErrorRollsBackEarlierClaim(true);
	}

	@Test
	void beforeRemoveErrorRollsBackEarlierClaims() throws Exception {
		assertRemovalErrorRollsBackEarlierClaim(false);
	}

	private void assertRemovalErrorRollsBackEarlierClaim(boolean failDuringStart) throws Exception {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		DbTxAwareDataSource aware = new DbTxAwareDataSource("orders", new CloseTrackingDataSource(),
				Collections.<DbTxDataSourceAdapter>emptyList());
		dynamicDataSource.put("old-id", "orders", "Old", aware, 100);
		ClaimTrackingDecorator claimed = new ClaimTrackingDecorator();
		AssertionError lifecycleError = new AssertionError("lifecycle error");
		TestRegistry registry = registry(dynamicDataSource, new FixedBuilder(new CloseTrackingDataSource()),
				Arrays.<MagicDataSourceDecorator>asList(
						claimed, new ErrorLifecycleDecorator(failDuringStart, lifecycleError)));

		Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
				() -> registry.unregisterDirect("orders"));

		assertThat(failure).isSameAs(lifecycleError);
		assertThat(claimed.failureCount).isOne();
		try (DbTxAwareDataSource.Lease ignored = aware.acquireLease()) {
		}
	}

	private static DbTxDefinition localDefinition() {
		return new DbTxDefinition("local", Collections.singletonList("orders"), 60000, "registry-test");
	}

	private static CloseTrackingH2DataSource h2DataSource(String name) {
		DriverManagerDataSource delegate = new DriverManagerDataSource(
				"jdbc:h2:mem:dbtx_registry_" + name + "_" + UUID.randomUUID()
						+ ";DB_CLOSE_DELAY=-1", "sa", "");
		return new CloseTrackingH2DataSource(delegate);
	}

	private static int count(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForObject("select count(*) from tx_test", Integer.class);
	}

	private static DataSource findDataSource(MagicDynamicDataSource dynamicDataSource, String key) {
		for (MagicDynamicDataSource.DataSourceNode node : dynamicDataSource.datasourceNodes()) {
			if (key.equals(node.getKey())) {
				return node.getDataSource();
			}
		}
		return null;
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
			unregisterDirect(key, null);
		}

		private void unregisterDirect(String key, CountDownLatch attempting) {
			DataSourceInfo info = info("old-id", key);
			MappingNode<DataSourceInfo> node = new MappingNode<DataSourceInfo>(info);
			node.setMappingKey(key);
			if (attempting != null) {
				attempting.countDown();
			}
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

	private static final class SequenceBuilder extends DataSourceBuilder {

		private final DataSource[] dataSources;
		private int index;

		private SequenceBuilder(DataSource... dataSources) {
			this.dataSources = dataSources;
		}

		@Override
		DataSource buildDataSource(String type, Map<String, Object> properties) {
			return dataSources[index++];
		}
	}

	private static class BlockingDeleteMagicDynamicDataSource extends ManagedMagicDynamicDataSource {

		final CountDownLatch deleteEntered = new CountDownLatch(1);
		final CountDownLatch releaseDelete = new CountDownLatch(1);

		@Override
		public boolean delete(String key) {
			deleteEntered.countDown();
			await(releaseDelete, "delete release timed out");
			return super.delete(key);
		}
	}

	private static final class BlockingPublicationMagicDynamicDataSource
			extends BlockingDeleteMagicDynamicDataSource {

		private final CountDownLatch nodePublished = new CountDownLatch(1);
		private final CountDownLatch releasePut = new CountDownLatch(1);
		private boolean blockNextPut = true;

		@Override
		public synchronized DataSourceNode putAndGet(String id, String key, String name,
				DataSource dataSource, int maxRows) {
			DataSourceNode publishedNode = super.putAndGet(id, key, name, dataSource, maxRows);
			if (blockNextPut) {
				blockNextPut = false;
				nodePublished.countDown();
				await(releasePut, "put release timed out");
			}
			return publishedNode;
		}
	}

	private static final class HidingPublishedNodeMagicDynamicDataSource
			extends ManagedMagicDynamicDataSource {

		private boolean hideNextSnapshot;

		@Override
		public synchronized DataSourceNode putAndGet(String id, String key, String name,
				DataSource dataSource, int maxRows) {
			DataSourceNode publishedNode = super.putAndGet(id, key, name, dataSource, maxRows);
			hideNextSnapshot = true;
			return publishedNode;
		}

		@Override
		public java.util.Collection<DataSourceNode> datasourceNodes() {
			if (hideNextSnapshot) {
				hideNextSnapshot = false;
				return Collections.emptyList();
			}
			return super.datasourceNodes();
		}
	}

	private static final class EquivalentReplacementManagedMagicDynamicDataSource
			extends ManagedMagicDynamicDataSource {

		@Override
		public synchronized void put(String id, String key, String name,
				DataSource dataSource, int maxRows) {
			super.put(id, key, name, dataSource, maxRows);
			super.put(id, key, name, dataSource, maxRows);
		}

		@Override
		public synchronized DataSourceNode putAndGet(String id, String key, String name,
				DataSource dataSource, int maxRows) {
			DataSourceNode publishedNode = super.putAndGet(id, key, name, dataSource, maxRows);
			super.put(id, key, name, dataSource, maxRows);
			return publishedNode;
		}
	}

	private static final class EquivalentReplacementMagicDynamicDataSource
			extends MagicDynamicDataSource {

		@Override
		public void put(String id, String key, String name,
				DataSource dataSource, int maxRows) {
			super.put(id, key, name, dataSource, maxRows);
			super.put(id, key, name, dataSource, maxRows);
		}
	}

	private static final class PerRegistrationStageDecorator implements MagicDataSourceDecorator {

		private final List<DataSource> createdStages = new ArrayList<DataSource>();
		private final Map<String, DataSource> removedStages = new HashMap<String, DataSource>();

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			CloseTrackingDataSource stage = new CloseTrackingDataSource();
			createdStages.add(stage);
			return stage;
		}

		@Override
		public void beforeRemove(String key, DataSource dataSource) {
			removedStages.put(key, dataSource);
		}
	}

	private static final class SharedFinalDataSourceDecorator implements MagicDataSourceDecorator {

		private final DataSource sharedDataSource;

		private SharedFinalDataSourceDecorator(DataSource sharedDataSource) {
			this.sharedDataSource = sharedDataSource;
		}

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			return sharedDataSource;
		}
	}

	private static final class CascadingStageDecorator implements MagicDataSourceDecorator {

		private final List<CascadingDataSource> stages = new ArrayList<CascadingDataSource>();
		private final Map<String, DataSource> removedStages = new HashMap<String, DataSource>();
		private int cleanupCount;

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			CascadingDataSource stage = new CascadingDataSource(dataSource);
			stages.add(stage);
			return stage;
		}

		@Override
		public void beforeRemove(String key, DataSource dataSource) {
			removedStages.put(key, dataSource);
		}

		@Override
		public void cleanupAfterDecorationFailure(String key, DataSource dataSource) throws Exception {
			cleanupCount++;
			((AutoCloseable) dataSource).close();
		}
	}

	private static final class CascadingDataSource extends DelegatingDataSource implements Closeable {

		private int closeCount;

		private CascadingDataSource(DataSource delegate) {
			super(delegate);
		}

		@Override
		public void close() throws IOException {
			closeCount++;
			((Closeable) getTargetDataSource()).close();
		}
	}

	private static final class AvailableDataSource implements DataSource, Closeable {

		private final Connection connection = mock(Connection.class);
		private int closeCount;

		@Override
		public Connection getConnection() throws SQLException {
			if (closeCount > 0) {
				throw new SQLException("closed");
			}
			return connection;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return getConnection();
		}

		@Override
		public void close() {
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

	private static final class ClaimTrackingDecorator implements MagicDataSourceDecorator {

		private int failureCount;

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			return dataSource;
		}

		@Override
		public void startRemove(String key, DataSource dataSource) {
			try {
				((DbTxAwareDataSource) dataSource).startRemove();
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void afterRemoveFailure(String key, DataSource dataSource) {
			failureCount++;
			((DbTxAwareDataSource) dataSource).cancelRemove();
		}
	}

	private static final class ErrorLifecycleDecorator implements MagicDataSourceDecorator {

		private final boolean failDuringStart;
		private final Error failure;

		private ErrorLifecycleDecorator(boolean failDuringStart, Error failure) {
			this.failDuringStart = failDuringStart;
			this.failure = failure;
		}

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			return dataSource;
		}

		@Override
		public void startRemove(String key, DataSource dataSource) {
			if (failDuringStart) {
				throw failure;
			}
		}

		@Override
		public void beforeRemove(String key, DataSource dataSource) {
			if (!failDuringStart) {
				throw failure;
			}
		}
	}

	private static void await(CountDownLatch latch, String timeoutMessage) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException(timeoutMessage);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted", e);
		}
	}

	private static Thread.State awaitBlockedOrDeleteEntered(Thread worker,
			CountDownLatch deleteEntered) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		Thread.State state = worker.getState();
		while (state != Thread.State.BLOCKED && deleteEntered.getCount() != 0) {
			if (System.nanoTime() >= deadline) {
				throw new AssertionError("removal worker did not block or enter delete; state="
						+ state + ", deleteEntered=" + deleteEntered.getCount());
			}
			Thread.yield();
			state = worker.getState();
		}
		return state;
	}

	private static final class PreparedVetoAdapter implements DbTxDataSourceAdapter {

		@Override
		public String getCoordinator() {
			return "prepared";
		}

		@Override
		public DataSource prepare(String key, DataSource dataSource) {
			return dataSource;
		}

		@Override
		public void beforeRemove(DataSource dataSource) {
			throw new IllegalStateException("prepared stage veto");
		}

		@Override
		public void close(DataSource dataSource) {
		}
	}

	private static final class OuterDataSourceDecorator implements MagicDataSourceDecorator {

		private final List<DbTxAwareDataSource> dbTxStages = new ArrayList<DbTxAwareDataSource>();

		@Override
		public DataSource decorate(String key, DataSource dataSource) {
			dbTxStages.add((DbTxAwareDataSource) dataSource);
			return new OuterDataSource(dataSource);
		}
	}

	private static final class OuterDataSource extends DelegatingDataSource implements Closeable {

		private OuterDataSource(DataSource delegate) {
			super(delegate);
		}

		@Override
		public void close() throws IOException {
			((Closeable) getTargetDataSource()).close();
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

	private static final class CloseTrackingH2DataSource extends DelegatingDataSource implements Closeable {

		private int closeCount;

		private CloseTrackingH2DataSource(DataSource delegate) {
			super(delegate);
		}

		@Override
		public void close() {
			closeCount++;
		}
	}
}
