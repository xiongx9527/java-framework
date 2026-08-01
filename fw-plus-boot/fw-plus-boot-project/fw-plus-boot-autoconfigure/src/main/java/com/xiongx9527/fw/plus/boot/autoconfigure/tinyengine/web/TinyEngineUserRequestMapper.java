package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePasswordResetRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineRegistrationRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantCreateRequest;

import java.util.Map;

public final class TinyEngineUserRequestMapper {

	private TinyEngineUserRequestMapper() {
	}

	public static TinyEngineLoginRequest toLoginRequest(Map<String, Object> body) {
		return new TinyEngineLoginRequest(string(body, "username"), string(body, "password"), string(body, "tenantId"));
	}

	public static TinyEngineRegistrationRequest toRegistrationRequest(Map<String, Object> body) {
		return new TinyEngineRegistrationRequest(string(body, "username"), string(body, "displayName"),
				string(body, "password"), string(body, "tenantId"));
	}

	public static TinyEnginePasswordResetRequest toPasswordResetRequest(Map<String, Object> body) {
		return new TinyEnginePasswordResetRequest(string(body, "username"), string(body, "tenantId"),
				string(body, "verificationCode"), string(body, "password"));
	}

	public static TinyEngineTenantCreateRequest toTenantCreateRequest(Map<String, Object> body) {
		String tenantId = string(body, "tenantId");
		if (tenantId == null) {
			tenantId = string(body, "id");
		}
		String tenantName = string(body, "tenantName");
		if (tenantName == null) {
			tenantName = string(body, "nameCn");
		}
		if (tenantName == null) {
			tenantName = string(body, "nameEn");
		}
		return new TinyEngineTenantCreateRequest(tenantId, tenantName);
	}

	public static String string(Map<String, Object> body, String key) {
		if (body == null || !body.containsKey(key) || body.get(key) == null) {
			return null;
		}
		return String.valueOf(body.get(key));
	}
}
