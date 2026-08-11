package com.xiongx9527.fw.plus.tinyengine.support.auth;

public enum AppRoleCode {
	APP_OWNER("app_owner"),
	APP_ADMIN("app_admin"),
	APP_DEV("app_dev");

	private final String code;

	AppRoleCode(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static AppRoleCode fromCode(String code) {
		for (AppRoleCode role : values()) {
			if (role.code.equals(code)) {
				return role;
			}
		}
		return null;
	}
}
