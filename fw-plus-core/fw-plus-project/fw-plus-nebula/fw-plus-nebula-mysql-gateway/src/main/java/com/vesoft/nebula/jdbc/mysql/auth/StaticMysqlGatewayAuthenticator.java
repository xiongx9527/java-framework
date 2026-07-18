package com.vesoft.nebula.jdbc.mysql.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticMysqlGatewayAuthenticator implements MysqlGatewayAuthenticator {
	private final Map<String, MysqlGatewayUser> users;

	public StaticMysqlGatewayAuthenticator(List<MysqlGatewayUser> users) {
		this.users = new HashMap<String, MysqlGatewayUser>();
		if (users != null) {
			for (MysqlGatewayUser user : users) {
				this.users.put(user.getUsername(), user);
			}
		}
	}

	public MysqlAuthenticationResult authenticatePlain(String username, String password, String database) {
		MysqlGatewayUser user = users.get(username);
		if (user == null || !user.getPassword().equals(password) || !user.allowsDatabase(database)) {
			return MysqlAuthenticationResult.failure("Access denied");
		}
		return MysqlAuthenticationResult.success(user);
	}

	@Override
	public MysqlAuthenticationResult authenticateNative(String username, byte[] seed, byte[] authResponse, String database) {
		MysqlGatewayUser user = users.get(username);
		if (user == null || !user.allowsDatabase(database) || !MysqlNativePassword.matches(user.getPassword(), seed, authResponse)) {
			return MysqlAuthenticationResult.failure("Access denied");
		}
		return MysqlAuthenticationResult.success(user);
	}
}
