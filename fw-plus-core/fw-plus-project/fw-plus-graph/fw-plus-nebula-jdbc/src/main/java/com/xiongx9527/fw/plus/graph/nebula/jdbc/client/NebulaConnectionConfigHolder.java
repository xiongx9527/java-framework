package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.config.NebulaConnectionConfig;

public class NebulaConnectionConfigHolder {
	private final NebulaConnectionConfig config;

	public NebulaConnectionConfigHolder(NebulaConnectionConfig config) {
		this.config = config;
	}

	public NebulaConnectionConfig getConfig() {
		return config;
	}
}
