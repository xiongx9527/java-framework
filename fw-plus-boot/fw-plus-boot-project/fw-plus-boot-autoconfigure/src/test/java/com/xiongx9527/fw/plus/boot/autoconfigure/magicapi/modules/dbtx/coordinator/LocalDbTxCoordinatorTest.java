package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LocalDbTxCoordinatorTest {

	@AfterEach
	void verifyThreadIsClean() {
		assertNoSpringResources();
	}

	@Test
	void keepsTheLegacyNoCauseExceptionConstructorOpenForInitCause() {
		DbTxException exception = new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, "invalid");
		IllegalArgumentException cause = new IllegalArgumentException("cause");

		exception.initCause(cause);

		assertThat(exception.getCause()).isSameAs(cause);
		assertThat(exception.getOutcome()).isNull();
	}

	@Test
	void beginsInSortedOrderAndCommitsInReverseOrder() throws Throwable {
		Fixture fixture = fixture();

		assertThat(fixture.coordinator.getName()).isEqualTo("local");
		Object result = fixture.coordinator.execute(definition("b", "a"), () -> {
			assertThat(TransactionSynchronizationManager.hasResource(fixture.a)).isTrue();
			assertThat(TransactionSynchronizationManager.hasResource(fixture.b)).isTrue();
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:commit", "a:commit");
		assertNoSpringResources();
	}

	@Test
	void rollsBackInReverseOrderAndRethrowsTheOriginalCallbackFailure() {
		Fixture fixture = fixture();
		IllegalStateException callbackFailure = new IllegalStateException("callback failed");

		Throwable thrown = catchThrowable(() -> fixture.coordinator.execute(
				definition("b", "a"), () -> {
					throw callbackFailure;
				}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:rollback", "a:rollback");
		assertNoSpringResources();
	}

	@Test
	void rollsBackTheFirstTransactionWhenTheSecondBeginFails() {
		Fixture fixture = fixture();
		fixture.b.failBegin = true;

		DbTxException thrown = (DbTxException) catchThrowable(() -> fixture.coordinator.execute(
				definition("b", "a"), () -> "unused"));

		assertThat(thrown.getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_BEGIN_FAILED);
		assertThat(thrown.getCause()).hasMessageContaining("b begin failed");
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "a:rollback");
		assertNoSpringResources();
	}

	@Test
	void attachesRollbackFailuresWithoutMaskingTheCallbackFailure() {
		Fixture fixture = fixture();
		fixture.b.failRollback = true;
		IllegalArgumentException callbackFailure = new IllegalArgumentException("callback failed");

		Throwable thrown = catchThrowable(() -> fixture.coordinator.execute(
				definition("a", "b"), () -> {
					throw callbackFailure;
				}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(thrown.getSuppressed()).hasSize(1);
		assertThat(thrown.getSuppressed()[0]).hasMessageContaining("b rollback failed");
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:rollback", "a:rollback");
		assertNoSpringResources();
	}

	@Test
	void reportsFirstReverseCommitFailureAndRollsBackOnlyPendingTransactions() {
		Fixture fixture = fixture();
		fixture.b.failCommit = true;

		DbTxException thrown = (DbTxException) catchThrowable(() -> fixture.coordinator.execute(
				definition("a", "b"), () -> "ok"));

		assertThat(thrown.getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_COMMIT_FAILED);
		assertOutcome(thrown.getOutcome(), "b", Collections.<String>emptyList(),
				Collections.singletonList("a"), Collections.<String>emptyList(),
				Collections.singletonList("b"));
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:commit", "a:rollback");
		assertNoSpringResources();
	}

	@Test
	void reportsSecondReverseCommitFailureAsHeuristicMixedWithoutRollingBackCommittedDataSource() {
		Fixture fixture = fixture();
		fixture.a.failCommit = true;

		DbTxException thrown = (DbTxException) catchThrowable(() -> fixture.coordinator.execute(
				definition("a", "b"), () -> "ok"));

		assertThat(thrown.getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_HEURISTIC_MIXED);
		assertOutcome(thrown.getOutcome(), "a", Collections.singletonList("b"),
				Collections.<String>emptyList(), Collections.<String>emptyList(),
				Collections.singletonList("a"));
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:commit", "a:commit");
		assertNoSpringResources();
	}

	@Test
	void reportsPendingRollbackFailureInTheCommitOutcome() {
		Fixture fixture = fixture();
		fixture.b.failCommit = true;
		fixture.a.failRollback = true;

		DbTxException thrown = (DbTxException) catchThrowable(() -> fixture.coordinator.execute(
				definition("a", "b"), () -> "ok"));

		assertThat(thrown.getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_COMMIT_FAILED);
		assertOutcome(thrown.getOutcome(), "b", Collections.<String>emptyList(),
				Collections.<String>emptyList(), Collections.singletonList("a"),
				Collections.singletonList("b"));
		assertThat(thrown.getSuppressed()).hasSize(1);
		assertThat(thrown.getSuppressed()[0]).hasMessageContaining("a rollback failed");
		assertThat(fixture.events).containsExactly("a:begin", "b:begin", "b:commit", "a:rollback");
		assertNoSpringResources();
	}

	@Test
	void rejectsAnAmbientSpringTransactionBeforeBeginningAnyLocalTransaction() {
		Fixture fixture = fixture();
		DriverManagerDataSource outerDataSource = h2DataSource("outer");
		DataSourceTransactionManager outerManager = new DataSourceTransactionManager(outerDataSource);
		TransactionStatus outer = outerManager.getTransaction(new DefaultTransactionDefinition());
		try {
			DbTxException thrown = (DbTxException) catchThrowable(() -> fixture.coordinator.execute(
					definition("a", "b"), () -> "unused"));

			assertThat(thrown.getDbTxCode()).isEqualTo(DbTxErrorCode.DBTX_AMBIENT_TRANSACTION);
			assertThat(fixture.events).isEmpty();
			assertThat(TransactionSynchronizationManager.hasResource(outerDataSource)).isTrue();
		} finally {
			outerManager.rollback(outer);
		}
		assertNoSpringResources();
	}

	private static void assertOutcome(DbTxOutcome outcome, String failedDataSource,
			List<String> committed, List<String> rolledBack, List<String> rollbackFailed,
			List<String> unknown) {
		assertThat(outcome).isNotNull();
		assertThat(outcome.getPhase()).isEqualTo("commit");
		assertThat(outcome.getCoordinator()).isEqualTo("local");
		assertThat(outcome.getFailedDataSource()).isEqualTo(failedDataSource);
		assertThat(outcome.getCommitted()).containsExactlyElementsOf(committed);
		assertThat(outcome.getRolledBack()).containsExactlyElementsOf(rolledBack);
		assertThat(outcome.getRollbackFailed()).containsExactlyElementsOf(rollbackFailed);
		assertThat(outcome.getUnknown()).containsExactlyElementsOf(unknown);
	}

	private static Fixture fixture() {
		List<String> events = new ArrayList<>();
		FaultInjectingDataSource a = new FaultInjectingDataSource("a", h2DataSource("a"), events);
		FaultInjectingDataSource b = new FaultInjectingDataSource("b", h2DataSource("b"), events);
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		dynamicDataSource.put("b", b);
		dynamicDataSource.put("a", a);
		return new Fixture(new LocalDbTxCoordinator(dynamicDataSource), a, b, events);
	}

	private static DriverManagerDataSource h2DataSource(String name) {
		return new DriverManagerDataSource("jdbc:h2:mem:dbtx_local_" + name + "_" + UUID.randomUUID()
				+ ";DB_CLOSE_DELAY=-1", "sa", "");
	}

	private static DbTxDefinition definition(String... keys) {
		return new DbTxDefinition("local", Arrays.asList(keys), 60000, "test");
	}

	private static void assertNoSpringResources() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	private static final class Fixture {

		private final LocalDbTxCoordinator coordinator;
		private final FaultInjectingDataSource a;
		private final FaultInjectingDataSource b;
		private final List<String> events;

		private Fixture(LocalDbTxCoordinator coordinator, FaultInjectingDataSource a,
				FaultInjectingDataSource b, List<String> events) {
			this.coordinator = coordinator;
			this.a = a;
			this.b = b;
			this.events = events;
		}
	}

	private static final class FaultInjectingDataSource implements DataSource {

		private final String key;
		private final DataSource delegate;
		private final List<String> events;
		private boolean failBegin;
		private boolean failCommit;
		private boolean failRollback;

		private FaultInjectingDataSource(String key, DataSource delegate, List<String> events) {
			this.key = key;
			this.delegate = delegate;
			this.events = events;
		}

		@Override
		public Connection getConnection() throws SQLException {
			events.add(key + ":begin");
			if (failBegin) {
				throw new SQLException(key + " begin failed");
			}
			return proxy(delegate.getConnection());
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			events.add(key + ":begin");
			if (failBegin) {
				throw new SQLException(key + " begin failed");
			}
			return proxy(delegate.getConnection(username, password));
		}

		private Connection proxy(Connection connection) {
			return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
					new Class<?>[]{Connection.class}, (Object proxy, Method method, Object[] args) -> {
						if ("commit".equals(method.getName())) {
							events.add(key + ":commit");
							if (failCommit) {
								throw new SQLException(key + " commit failed");
							}
						} else if ("rollback".equals(method.getName())) {
							events.add(key + ":rollback");
							if (failRollback) {
								throw new SQLException(key + " rollback failed");
							}
						}
						try {
							return method.invoke(connection, args);
						} catch (InvocationTargetException exception) {
							throw exception.getTargetException();
						}
					});
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return delegate.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			delegate.setLogWriter(out);
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			delegate.setLoginTimeout(seconds);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return delegate.getLoginTimeout();
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return delegate.getParentLogger();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			if (iface.isInstance(this)) {
				return iface.cast(this);
			}
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return iface.isInstance(this) || delegate.isWrapperFor(iface);
		}
	}
}
