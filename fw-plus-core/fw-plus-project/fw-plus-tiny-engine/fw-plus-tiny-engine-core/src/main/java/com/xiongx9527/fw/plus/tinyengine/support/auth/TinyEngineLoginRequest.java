package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEngineLoginRequest {

	private final String username;
	private final String password;
	private final String tenantId;

	public TinyEngineLoginRequest(String username, String password, String tenantId) {
		this.username = username;
		this.password = password;
		this.tenantId = tenantId;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getTenantId() {
		return tenantId;
	}
}
