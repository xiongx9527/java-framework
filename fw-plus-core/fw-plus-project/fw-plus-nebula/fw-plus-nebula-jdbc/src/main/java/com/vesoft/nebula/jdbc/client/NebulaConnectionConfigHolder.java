package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;

public class NebulaConnectionConfigHolder {
	private final NebulaConnectionConfig config;

	public NebulaConnectionConfigHolder(NebulaConnectionConfig config) {
		this.config = config;
	}

	public NebulaConnectionConfig getConfig() {
		return config;
	}
}
