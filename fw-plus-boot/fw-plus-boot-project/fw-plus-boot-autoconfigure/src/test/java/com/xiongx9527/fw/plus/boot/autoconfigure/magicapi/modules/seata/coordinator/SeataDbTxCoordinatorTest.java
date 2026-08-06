package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

class SeataDbTxCoordinatorTest {

	@Test
	void preparesEveryParticipantBeforeCreatingAndBeginningALauncher() throws Throwable {
		List<String> events = new ArrayList<>();
		RecordingAdapter adapter = new RecordingAdapter(events);
		MagicDynamicDataSource dynamicDataSource = dynamicDataSource(adapter, "a", "b");
		RecordingTransaction transaction = new RecordingTransaction(true, events);
		SeataDbTxCoordinator coordinator = new SeataDbTxCoordinator(dynamicDataSource,
				() -> {
					events.add("transaction");
					return transaction;
				});

		Object result = coordinator.execute(definition("a", "b"), () -> {
			events.add("callback");
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(coordinator.getName()).isEqualTo("seata");
		assertThat(events).containsExactly("a:prepare", "b:prepare", "transaction",
				"begin:60000:magic-api-dbtx-test", "callback", "commit");
	}

	@Test
	void reportsParticipantPreparationFailureBeforeTransactionLookupOrCallback() {
		List<String> events = new ArrayList<>();
		RecordingAdapter adapter = new RecordingAdapter(events);
		adapter.failingKey = "b";
		MagicDynamicDataSource dynamicDataSource = dynamicDataSource(adapter, "a", "b");
		SeataDbTxCoordinator coordinator = new SeataDbTxCoordinator(dynamicDataSource,
				() -> {
					events.add("transaction");
					return new RecordingTransaction(true, events);
				});

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition("a", "b"), () -> {
			events.add("callback");
			return "unused";
		}));

		assertThat(thrown).isInstanceOf(DbTxException.class);
		assertThat(((DbTxException) thrown).getCode()).isEqualTo(DbTxErrorCode.DBTX_BEGIN_FAILED);
		assertThat(thrown).hasMessageContaining("b").hasRootCauseMessage("unsupported database");
		assertThat(events).containsExactly("a:prepare", "b:prepare");
	}

	@Test
	void rollsBackALauncherAndRethrowsTheOriginalCallbackFailure() {
		List<String> events = new ArrayList<>();
		RecordingTransaction transaction = new RecordingTransaction(true, events);
		SeataDbTxCoordinator coordinator = coordinator(transaction, events);
		IllegalStateException callbackFailure = new IllegalStateException("callback failed");

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition("a"), () -> {
			throw callbackFailure;
		}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(events).containsExactly("a:prepare", "transaction",
				"begin:60000:magic-api-dbtx-test", "rollback");
	}

	@Test
	void suppressesLauncherRollbackFailureOntoTheOriginalCallbackFailure() {
		List<String> events = new ArrayList<>();
		RecordingTransaction transaction = new RecordingTransaction(true, events);
		transaction.rollbackFailure = new IllegalStateException("rollback failed");
		SeataDbTxCoordinator coordinator = coordinator(transaction, events);
		IllegalArgumentException callbackFailure = new IllegalArgumentException("callback failed");

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition("a"), () -> {
			throw callbackFailure;
		}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(thrown.getSuppressed()).containsExactly(transaction.rollbackFailure);
		assertThat(events).containsExactly("a:prepare", "transaction",
				"begin:60000:magic-api-dbtx-test", "rollback");
	}

	@Test
	void participantReturnsNormallyWithoutOwningBeginCommitOrRollback() throws Throwable {
		List<String> events = new ArrayList<>();
		RecordingTransaction transaction = new RecordingTransaction(false, events);
		SeataDbTxCoordinator coordinator = coordinator(transaction, events);

		Object result = coordinator.execute(definition("a"), () -> {
			events.add("callback");
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(events).containsExactly("a:prepare", "transaction", "callback");
	}

	@Test
	void participantRethrowsCallbackFailureWithoutEndingTheExistingTransaction() {
		List<String> events = new ArrayList<>();
		RecordingTransaction transaction = new RecordingTransaction(false, events);
		SeataDbTxCoordinator coordinator = coordinator(transaction, events);
		IllegalStateException callbackFailure = new IllegalStateException("callback failed");

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition("a"), () -> {
			throw callbackFailure;
		}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(events).containsExactly("a:prepare", "transaction");
	}

	@Test
	void wrapsCommitFailureWithXidAndBestStatusWithoutBlindRollback() {
		List<String> events = new ArrayList<>();
		RecordingTransaction transaction = new RecordingTransaction(true, events);
		transaction.xid = "10.0.0.8:8091:12345";
		transaction.status = GlobalStatus.CommitFailed;
		transaction.commitFailure = new IllegalStateException("commit failed");
		SeataDbTxCoordinator coordinator = coordinator(transaction, events);

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition("a"), () -> "ok"));

		assertThat(thrown).isInstanceOf(DbTxException.class);
		DbTxException exception = (DbTxException) thrown;
		assertThat(exception.getCode()).isEqualTo(DbTxErrorCode.DBTX_COMMIT_FAILED);
		assertThat(exception.getCause()).isSameAs(transaction.commitFailure);
		assertThat(exception).hasMessageContaining("10.0.0.8:8091:12345")
				.hasMessageContaining("CommitFailed");
		assertThat(events).containsExactly("a:prepare", "transaction",
				"begin:60000:magic-api-dbtx-test", "commit", "xid", "status");
	}

	private static SeataDbTxCoordinator coordinator(RecordingTransaction transaction,
			List<String> events) {
		RecordingAdapter adapter = new RecordingAdapter(events);
		return new SeataDbTxCoordinator(dynamicDataSource(adapter, "a"), () -> {
			events.add("transaction");
			return transaction;
		});
	}

	private static MagicDynamicDataSource dynamicDataSource(RecordingAdapter adapter,
			String... keys) {
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		for (String key : keys) {
			DbTxAwareDataSource aware = new DbTxAwareDataSource(key, mock(DataSource.class),
					Collections.<DbTxDataSourceAdapter>singletonList(adapter));
			dynamicDataSource.put(key, aware);
		}
		return dynamicDataSource;
	}

	private static DbTxDefinition definition(String... keys) {
		return new DbTxDefinition("seata", Arrays.asList(keys), 60000,
				"magic-api-dbtx-test");
	}

	private static final class RecordingAdapter implements DbTxDataSourceAdapter {

		private final List<String> events;
		private String failingKey;

		private RecordingAdapter(List<String> events) {
			this.events = events;
		}

		@Override
		public String getCoordinator() {
			return "seata";
		}

		@Override
		public DataSource prepare(String key, DataSource dataSource) {
			events.add(key + ":prepare");
			if (key.equals(failingKey)) {
				throw new IllegalStateException("unsupported database");
			}
			return dataSource;
		}

		@Override
		public void close(DataSource dataSource) {
		}
	}

	private static final class RecordingTransaction implements SeataTransaction {

		private final boolean launcher;
		private final List<String> events;
		private String xid = "xid";
		private GlobalStatus status = GlobalStatus.Begin;
		private RuntimeException commitFailure;
		private RuntimeException rollbackFailure;

		private RecordingTransaction(boolean launcher, List<String> events) {
			this.launcher = launcher;
			this.events = events;
		}

		@Override
		public boolean isLauncher() {
			return launcher;
		}

		@Override
		public void begin(int timeoutMs, String name) {
			events.add("begin:" + timeoutMs + ":" + name);
		}

		@Override
		public void commit() {
			events.add("commit");
			if (commitFailure != null) {
				throw commitFailure;
			}
		}

		@Override
		public void rollback() {
			events.add("rollback");
			if (rollbackFailure != null) {
				throw rollbackFailure;
			}
		}

		@Override
		public String getXid() {
			events.add("xid");
			return xid;
		}

		@Override
		public GlobalStatus getGlobalStatus() {
			events.add("status");
			return status;
		}
	}
}
