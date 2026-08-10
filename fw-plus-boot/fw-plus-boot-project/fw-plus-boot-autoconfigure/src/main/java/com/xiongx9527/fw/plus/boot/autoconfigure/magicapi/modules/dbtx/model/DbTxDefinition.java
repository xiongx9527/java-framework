package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DbTxDefinition {

	private final String coordinator;

	private final List<String> dataSourceKeys;

	private final int timeoutMs;

	private final String name;

	public DbTxDefinition(String coordinator, List<String> dataSourceKeys, int timeoutMs, String name) {
		this.coordinator = coordinator;
		this.dataSourceKeys = Collections.unmodifiableList(new ArrayList<>(dataSourceKeys));
		this.timeoutMs = timeoutMs;
		this.name = name;
	}

	public String getCoordinator() {
		return coordinator;
	}

	public List<String> getDataSourceKeys() {
		return dataSourceKeys;
	}

	public int getTimeoutMs() {
		return timeoutMs;
	}

	public String getName() {
		return name;
	}
}
