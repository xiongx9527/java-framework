package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DbTxOutcome {

	private final String phase;

	private final String coordinator;

	private final String failedDataSource;

	private final List<String> committed;

	private final List<String> rolledBack;

	private final List<String> rollbackFailed;

	private final List<String> unknown;

	public DbTxOutcome(String phase, String coordinator, String failedDataSource,
			List<String> committed, List<String> rolledBack, List<String> rollbackFailed,
			List<String> unknown) {
		this.phase = phase;
		this.coordinator = coordinator;
		this.failedDataSource = failedDataSource;
		this.committed = immutableCopy(committed);
		this.rolledBack = immutableCopy(rolledBack);
		this.rollbackFailed = immutableCopy(rollbackFailed);
		this.unknown = immutableCopy(unknown);
	}

	public String getPhase() {
		return phase;
	}

	public String getCoordinator() {
		return coordinator;
	}

	public String getFailedDataSource() {
		return failedDataSource;
	}

	public List<String> getCommitted() {
		return committed;
	}

	public List<String> getRolledBack() {
		return rolledBack;
	}

	public List<String> getRollbackFailed() {
		return rollbackFailed;
	}

	public List<String> getUnknown() {
		return unknown;
	}

	private static List<String> immutableCopy(List<String> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(values));
	}
}
