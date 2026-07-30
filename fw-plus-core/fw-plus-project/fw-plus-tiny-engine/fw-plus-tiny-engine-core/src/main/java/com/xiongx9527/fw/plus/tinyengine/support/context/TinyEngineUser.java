package com.xiongx9527.fw.plus.tinyengine.support.context;

public class TinyEngineUser {

	private final String userId;
	private final String userName;
	private final String tenantId;
	private final String renterId;
	private final Integer platformId;

	public TinyEngineUser(String userId, String userName, String tenantId, String renterId, Integer platformId) {
		this.userId = userId;
		this.userName = userName;
		this.tenantId = tenantId;
		this.renterId = renterId;
		this.platformId = platformId;
	}

	public String getUserId() { return userId; }
	public String getUserName() { return userName; }
	public String getTenantId() { return tenantId; }
	public String getRenterId() { return renterId; }
	public Integer getPlatformId() { return platformId; }
}
