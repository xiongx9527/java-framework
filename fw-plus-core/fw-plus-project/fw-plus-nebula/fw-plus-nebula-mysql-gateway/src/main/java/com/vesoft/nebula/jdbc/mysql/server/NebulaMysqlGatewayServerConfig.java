package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.security.MysqlGatewaySecurityPolicy;
import com.vesoft.nebula.jdbc.mysql.security.MysqlSslMode;

public class NebulaMysqlGatewayServerConfig {
	private final String host;
	private final int port;
	private final MysqlGatewaySecurityPolicy securityPolicy;

	public NebulaMysqlGatewayServerConfig(String host, int port) {
		this(host, port, MysqlGatewaySecurityPolicy.allowAll());
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, MysqlGatewaySecurityPolicy securityPolicy) {
		this.host = host == null ? "0.0.0.0" : host;
		this.port = port;
		this.securityPolicy = securityPolicy == null ? MysqlGatewaySecurityPolicy.allowAll() : securityPolicy;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public MysqlGatewaySecurityPolicy getSecurityPolicy() {
		return securityPolicy;
	}

	public MysqlSslMode getSslMode() {
		return securityPolicy.getSslMode();
	}
}
