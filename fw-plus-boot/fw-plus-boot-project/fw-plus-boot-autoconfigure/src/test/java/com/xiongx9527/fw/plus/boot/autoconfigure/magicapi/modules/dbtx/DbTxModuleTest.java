package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCallback;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter.DbTxProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbTxModuleTest {

	@AfterEach
	void clearContext() {
		DbTxContextHolder.clear();
	}

	@Test
	void routesBlankCoordinatorToDefaultAndPreservesCallbackResult() throws Throwable {
		RecordingCoordinator local = new RecordingCoordinator("local");
		RecordingCoordinator seata = new RecordingCoordinator("seata");
		DbTxModule module = module("local", local, seata);

		assertThat(module.resolveCoordinator(null)).isEqualTo("local");
		assertThat(module.resolveCoordinator(" SEATA ")).isEqualTo("seata");
		assertThat(module.transaction(Arrays.asList("orders", "accounts"), callback("done"))).isEqualTo("done");
		assertThat(module.transaction(" SEATA ", Collections.singletonList("orders"), callback("global")))
				.isEqualTo("global");
		assertThat(local.executions).isEqualTo(1);
		assertThat(seata.executions).isEqualTo(1);
		assertThat(local.lastDefinition.getDataSourceKeys()).containsExactly("accounts", "orders");
	}

	@Test
	void defaultConfigurationOverridesRouteAndTimeout() throws Throwable {
		RecordingCoordinator local = new RecordingCoordinator("local");
		RecordingCoordinator seata = new RecordingCoordinator("seata");
		DbTxProperties properties = new DbTxProperties();
		properties.setDefaultCoordinator("seata");
		properties.setTimeoutMs(1234);
		DbTxModule module = new DbTxModule(new DbTxCoordinatorRegistry(Arrays.asList(local, seata)), properties);

		assertThat(module.transaction(Collections.singletonList("orders"), callback("ok"))).isEqualTo("ok");
		assertThat(local.executions).isZero();
		assertThat(seata.executions).isEqualTo(1);
		assertThat(seata.lastDefinition.getTimeoutMs()).isEqualTo(1234);
	}

	@Test
	void rejectsUnknownCoordinatorAndInvalidDataSourceKeys() {
		DbTxModule module = module("local", new RecordingCoordinator("local"));

		assertThatThrownBy(() -> module.transaction("unknown", Arrays.asList("a"), callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction(Collections.<String>emptyList(), callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction((List<String>) null, callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction(Arrays.asList("a", null), callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction(Arrays.asList("a", " a "), callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction(Arrays.asList("a", " "), callback("unused")))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction(Collections.singletonList("a"), null))
				.isInstanceOf(DbTxException.class);
	}

	@Test
	void transactionDefinitionKeepsAnImmutableKeySnapshot() {
		List<String> keys = new ArrayList<>(Arrays.asList("accounts", "orders"));
		DbTxDefinition definition = new DbTxDefinition("local", keys, 60000, "test");

		keys.clear();

		assertThat(definition.getDataSourceKeys()).containsExactly("accounts", "orders");
		assertThatThrownBy(() -> definition.getDataSourceKeys().add("audit"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void nestedSubsetUsesOuterCoordinatorAndDoesNotOpenAnotherTransaction() throws Throwable {
		RecordingCoordinator local = new RecordingCoordinator("local");
		DbTxModule module = module("local", local);

		Object result = module.transaction("local", Arrays.asList("orders", "accounts"), ignored ->
				transactionUnchecked(module, "local", Collections.singletonList("accounts"), callback("inner")));

		assertThat(result).isEqualTo("inner");
		assertThat(local.executions).isEqualTo(1);
		assertThat(local.lastDefinition.getDataSourceKeys()).containsExactly("accounts", "orders");
	}

	@Test
	void caughtInnerFailureMarksOuterContextRollbackOnly() throws Throwable {
		RecordingCoordinator local = new RecordingCoordinator("local");
		DbTxModule module = module("local", local);

		assertThat(module.transaction(Collections.singletonList("orders"), ignored -> {
			try {
				transactionUnchecked(module, Collections.singletonList("orders"), failingCallback());
			} catch (IllegalStateException expected) {
				return "recovered";
			}
			return "not-reached";
		})).isEqualTo("recovered");

		assertThat(local.rollbackOnlyWhenCallbackReturned).isTrue();
	}

	@Test
	void rejectsNestedScopeExpansionAndCoordinatorChanges() throws Throwable {
		RecordingCoordinator local = new RecordingCoordinator("local");
		RecordingCoordinator seata = new RecordingCoordinator("seata");
		DbTxModule module = module("local", local, seata);

		assertThatThrownBy(() -> module.transaction("local", Collections.singletonList("orders"), ignored ->
				transactionUnchecked(module, "local", Arrays.asList("orders", "accounts"), callback("unused"))))
				.isInstanceOf(DbTxException.class);
		assertThatThrownBy(() -> module.transaction("local", Collections.singletonList("orders"), ignored ->
				transactionUnchecked(module, "seata", Collections.singletonList("orders"), callback("unused"))))
				.isInstanceOf(DbTxException.class);
		assertThat(local.executions).isEqualTo(2);
		assertThat(seata.executions).isZero();
	}

	@Test
	void clearsThreadLocalAfterSuccessAndFailure() throws Throwable {
		DbTxModule module = module("local", new RecordingCoordinator("local"));

		module.transaction(Collections.singletonList("orders"), callback("ok"));
		assertThat(DbTxContextHolder.current()).isNull();

		assertThatThrownBy(() -> module.transaction(Collections.singletonList("orders"), failingCallback()))
				.isInstanceOf(IllegalStateException.class);
		assertThat(DbTxContextHolder.current()).isNull();
	}

	private DbTxModule module(String defaultCoordinator, DbTxCoordinator... coordinators) {
		DbTxProperties properties = new DbTxProperties();
		properties.setDefaultCoordinator(defaultCoordinator);
		return new DbTxModule(new DbTxCoordinatorRegistry(Arrays.asList(coordinators)), properties);
	}

	private Function<Object, Object> callback(final Object value) {
		return ignored -> value;
	}

	private Function<Object, Object> failingCallback() {
		return ignored -> {
			throw new IllegalStateException("boom");
		};
	}

	private Object transactionUnchecked(DbTxModule module, List<String> keys, Function<?, ?> callback) {
		return transactionUnchecked(module, null, keys, callback);
	}

	private Object transactionUnchecked(DbTxModule module, String coordinator, List<String> keys,
									Function<?, ?> callback) {
		try {
			return module.transaction(coordinator, keys, callback);
		} catch (Throwable throwable) {
			return DbTxModuleTest.<RuntimeException>throwUnchecked(throwable);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> Object throwUnchecked(Throwable throwable) throws T {
		throw (T) throwable;
	}

	private static final class RecordingCoordinator implements DbTxCoordinator {
		private final String name;
		private int executions;
		private DbTxDefinition lastDefinition;
		private boolean rollbackOnlyWhenCallbackReturned;

		private RecordingCoordinator(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object execute(DbTxDefinition definition, DbTxCallback callback) throws Throwable {
			executions++;
			lastDefinition = definition;
			Object result = callback.execute();
			rollbackOnlyWhenCallbackReturned = DbTxContextHolder.current().isRollbackOnly();
			return result;
		}
	}
}
