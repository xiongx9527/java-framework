package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEnginePasswordResetRequest {

	private final String username;
	private final String tenantId;
	private final String verificationCode;
	private final String newPassword;

	public TinyEnginePasswordResetRequest(String username, String tenantId, String verificationCode,
			String newPassword) {
		this.username = username;
		this.tenantId = tenantId;
		this.verificationCode = verificationCode;
		this.newPassword = newPassword;
	}

	public String getUsername() {
		return username;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public String getNewPassword() {
		return newPassword;
	}
}
