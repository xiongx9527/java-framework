package com.vesoft.nebula.jdbc.mysql.auth;

public class MysqlAuthenticationResult {
	private final boolean authenticated;
	private final MysqlGatewayUser user;
	private final String message;

	private MysqlAuthenticationResult(boolean authenticated, MysqlGatewayUser user, String message) {
		this.authenticated = authenticated;
		this.user = user;
		this.message = message;
	}

	public static MysqlAuthenticationResult success(MysqlGatewayUser user) {
		return new MysqlAuthenticationResult(true, user, "OK");
	}

	public static MysqlAuthenticationResult failure(String message) {
		return new MysqlAuthenticationResult(false, null, message);
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public MysqlGatewayUser getUser() {
		return user;
	}

	public String getMessage() {
		return message;
	}
}
