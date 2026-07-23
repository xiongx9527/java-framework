package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

public class NebulaHostAddress {
	private final String host;
	private final int port;

	public NebulaHostAddress(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
}
