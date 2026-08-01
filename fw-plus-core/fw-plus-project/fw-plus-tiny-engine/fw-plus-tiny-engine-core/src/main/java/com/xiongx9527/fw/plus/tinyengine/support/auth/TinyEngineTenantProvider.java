package com.xiongx9527.fw.plus.tinyengine.support.auth;

import java.util.List;

public interface TinyEngineTenantProvider {

	List<TinyEngineTenant> listTenants(TinyEnginePrincipal principal);

	TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId);

	TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request);
}
