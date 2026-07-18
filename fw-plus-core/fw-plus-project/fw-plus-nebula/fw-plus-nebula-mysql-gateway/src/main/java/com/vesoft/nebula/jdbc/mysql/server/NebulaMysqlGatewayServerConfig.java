package com.vesoft.nebula.jdbc.mysql.server;

public class NebulaMysqlGatewayServerConfig {
	private final String host;
	private final int port;

	public NebulaMysqlGatewayServerConfig(String host, int port) {
		this.host = host == null ? "0.0.0.0" : host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
