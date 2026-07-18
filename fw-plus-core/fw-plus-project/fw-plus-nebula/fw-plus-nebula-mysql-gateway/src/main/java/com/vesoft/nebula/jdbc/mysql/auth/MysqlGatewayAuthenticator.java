package com.vesoft.nebula.jdbc.mysql.auth;

public interface MysqlGatewayAuthenticator {
	MysqlAuthenticationResult authenticateNative(String username, byte[] seed, byte[] authResponse, String database);
}
