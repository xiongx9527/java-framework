package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinatorRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.starter.DbTxProperties;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

@MagicModule("dbtx")
public class DbTxModule {

	private final DbTxCoordinatorRegistry coordinatorRegistry;

	private final DbTxProperties properties;

	public DbTxModule(DbTxCoordinatorRegistry coordinatorRegistry, DbTxProperties properties) {
		if (coordinatorRegistry == null) {
			throw new IllegalArgumentException("Coordinator registry must not be null");
		}
		this.coordinatorRegistry = coordinatorRegistry;
		this.properties = properties == null ? new DbTxProperties() : properties;
	}

	public Object transaction(Collection<String> keys, Function<?, ?> callback) throws Throwable {
		return transaction(null, keys, callback);
	}

	public Object transaction(String coordinator, Collection<String> keys,
						  Function<?, ?> callback) throws Throwable {
		String resolvedCoordinator = resolveCoordinator(coordinator);
		DbTxContext current = DbTxContextHolder.current();
		if (current != null) {
			return executeNested(current, resolvedCoordinator, keys, callback);
		}

		validateCallback(callback);
		List<String> normalizedKeys = normalizeKeys(keys, true);
		DbTxCoordinator selectedCoordinator = coordinatorRegistry.getRequired(resolvedCoordinator);
		DbTxDefinition definition = new DbTxDefinition(resolvedCoordinator, normalizedKeys,
				properties.getTimeoutMs(), properties.getNamePrefix());

		DbTxContextHolder.open(definition);
		try {
			return selectedCoordinator.execute(definition, () -> callback.apply(null));
		} finally {
			DbTxContextHolder.clear();
		}
	}

	public String resolveCoordinator(String coordinator) {
		String candidate = coordinator;
		if (candidate == null || candidate.trim().isEmpty()) {
			candidate = properties.getDefaultCoordinator();
		}
		if (candidate == null || candidate.trim().isEmpty()) {
			throw validation("Coordinator must not be blank");
		}
		return candidate.trim().toLowerCase(Locale.ENGLISH);
	}

	private Object executeNested(DbTxContext context, String coordinator, Collection<String> keys,
							 Function<?, ?> callback) throws Throwable {
		boolean entered = false;
		try {
			validateCallback(callback);
			if (!context.getCoordinator().equals(coordinator)) {
				throw new DbTxException(DbTxErrorCode.DBTX_NESTED_MODE_MISMATCH,
						"Nested coordinator must match the outer transaction");
			}
			List<String> nestedKeys = normalizeKeys(keys, false);
			if (!context.getDataSourceKeys().containsAll(nestedKeys)) {
				throw new DbTxException(DbTxErrorCode.DBTX_NESTED_SCOPE_EXPANSION,
						"Nested data source keys must be a subset of the outer transaction");
			}
			context.enter();
			entered = true;
			return callback.apply(null);
		} catch (Throwable throwable) {
			context.markRollbackOnly();
			throw throwable;
		} finally {
			if (entered) {
				context.exit();
			}
		}
	}

	private List<String> normalizeKeys(Collection<String> keys, boolean sort) {
		if (keys == null || keys.isEmpty()) {
			throw validation("At least one data source key is required");
		}
		List<String> normalized = new ArrayList<>(keys.size());
		Set<String> unique = new HashSet<>();
		for (String key : keys) {
			if (key == null || key.trim().isEmpty()) {
				throw validation("Data source key must not be blank");
			}
			String normalizedKey = key.trim();
			if (!unique.add(normalizedKey)) {
				throw validation("Duplicate data source key: " + normalizedKey);
			}
			normalized.add(normalizedKey);
		}
		if (sort) {
			Collections.sort(normalized);
		}
		return normalized;
	}

	private void validateCallback(Function<?, ?> callback) {
		if (callback == null) {
			throw validation("Callback must not be null");
		}
	}

	private DbTxException validation(String message) {
		return new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, message);
	}
}
