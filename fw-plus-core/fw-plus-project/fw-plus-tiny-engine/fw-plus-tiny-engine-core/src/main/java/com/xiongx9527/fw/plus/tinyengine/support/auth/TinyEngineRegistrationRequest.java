package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEngineRegistrationRequest {

	private final String username;
	private final String displayName;
	private final String password;
	private final String tenantId;
	private final String requestHost;

	public TinyEngineRegistrationRequest(String username, String displayName, String password, String tenantId) {
		this(username, displayName, password, tenantId, null);
	}

	public TinyEngineRegistrationRequest(String username, String displayName, String password, String tenantId,
			String requestHost) {
		this.username = username;
		this.displayName = displayName;
		this.password = password;
		this.tenantId = tenantId;
		this.requestHost = requestHost;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPassword() {
		return password;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getRequestHost() {
		return requestHost;
	}
}
