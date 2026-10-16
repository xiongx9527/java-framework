package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

import java.io.Serializable;

public class SessionContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String tenantId;
	private final String appId;
	private final String agentId;
	private final String userId;
	private final String memoryId;

	public SessionContext(String tenantId, String appId, String agentId, String userId, String memoryId) {
		this.tenantId = tenantId;
		this.appId = appId;
		this.agentId = agentId;
		this.userId = userId;
		this.memoryId = memoryId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public String getAgentId() {
		return agentId;
	}

	public String getUserId() {
		return userId;
	}

	public String getMemoryId() {
		return memoryId;
	}

}