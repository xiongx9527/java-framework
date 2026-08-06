package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DbTxCoordinatorRegistry {

	private final Map<String, DbTxCoordinator> coordinators;

	public DbTxCoordinatorRegistry(Collection<? extends DbTxCoordinator> coordinators) {
		Map<String, DbTxCoordinator> registered = new LinkedHashMap<>();
		if (coordinators != null) {
			for (DbTxCoordinator coordinator : coordinators) {
				if (coordinator == null) {
					throw validation("Coordinator must not be null");
				}
				String name = normalizeName(coordinator.getName());
				if (name == null) {
					throw validation("Coordinator name must not be blank");
				}
				if (registered.put(name, coordinator) != null) {
					throw validation("Duplicate coordinator: " + name);
				}
			}
		}
		this.coordinators = Collections.unmodifiableMap(registered);
	}

	public DbTxCoordinator getRequired(String name) {
		String normalizedName = normalizeName(name);
		DbTxCoordinator coordinator = normalizedName == null ? null : coordinators.get(normalizedName);
		if (coordinator == null) {
			throw new DbTxException(DbTxErrorCode.DBTX_COORDINATOR_UNAVAILABLE,
					"Coordinator is unavailable: " + name);
		}
		return coordinator;
	}

	public boolean contains(String name) {
		String normalizedName = normalizeName(name);
		return normalizedName != null && coordinators.containsKey(normalizedName);
	}

	private static String normalizeName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}
		return name.trim().toLowerCase(Locale.ENGLISH);
	}

	private static DbTxException validation(String message) {
		return new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, message);
	}
}
