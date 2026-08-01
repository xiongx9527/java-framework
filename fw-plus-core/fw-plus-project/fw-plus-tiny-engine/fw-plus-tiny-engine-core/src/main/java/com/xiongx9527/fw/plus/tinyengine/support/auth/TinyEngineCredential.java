package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEngineCredential {

	private final String accessToken;
	private final String tenantId;

	public TinyEngineCredential(String accessToken, String tenantId) {
		this.accessToken = accessToken;
		this.tenantId = tenantId;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTenantId() {
		return tenantId;
	}
}
