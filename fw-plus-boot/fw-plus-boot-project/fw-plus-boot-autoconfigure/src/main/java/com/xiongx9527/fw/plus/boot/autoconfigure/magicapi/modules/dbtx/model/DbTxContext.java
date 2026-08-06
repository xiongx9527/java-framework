package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DbTxContext {

	private final DbTxDefinition definition;

	private final Set<String> dataSourceKeys;

	private int depth = 1;

	private boolean rollbackOnly;

	public DbTxContext(DbTxDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("Definition must not be null");
		}
		this.definition = definition;
		this.dataSourceKeys = Collections.unmodifiableSet(
				new LinkedHashSet<>(definition.getDataSourceKeys()));
	}

	public DbTxDefinition getDefinition() {
		return definition;
	}

	public String getCoordinator() {
		return definition.getCoordinator();
	}

	public Set<String> getDataSourceKeys() {
		return dataSourceKeys;
	}

	public boolean containsDataSource(String key) {
		return dataSourceKeys.contains(key);
	}

	public int getDepth() {
		return depth;
	}

	public void enter() {
		depth++;
	}

	public void exit() {
		if (depth <= 1) {
			throw new IllegalStateException("Cannot exit the outer transaction context");
		}
		depth--;
	}

	public boolean isRollbackOnly() {
		return rollbackOnly;
	}

	public void markRollbackOnly() {
		rollbackOnly = true;
	}
}
