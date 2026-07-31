package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEngineTenantCreateRequest {

	private final String tenantId;
	private final String tenantName;

	public TinyEngineTenantCreateRequest(String tenantId, String tenantName) {
		this.tenantId = tenantId;
		this.tenantName = tenantName;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getTenantName() {
		return tenantName;
	}
}
