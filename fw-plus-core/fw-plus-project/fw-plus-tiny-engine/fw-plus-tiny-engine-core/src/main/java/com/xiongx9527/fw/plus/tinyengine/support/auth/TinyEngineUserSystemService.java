package com.xiongx9527.fw.plus.tinyengine.support.auth;

import java.util.List;

public interface TinyEngineUserSystemService {

	TinyEngineLoginResult login(TinyEngineLoginRequest request);

	TinyEnginePrincipal currentUser(TinyEngineCredential credential);

	void logout(TinyEngineCredential credential);

	TinyEnginePrincipal register(TinyEngineRegistrationRequest request);

	void requestPasswordReset(TinyEnginePasswordResetRequest request);

	List<TinyEngineTenant> tenants(TinyEnginePrincipal principal);

	TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId);

	TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request);

	void check(TinyEnginePrincipal principal, String permission);
}
