package com.xiongx9527.fw.plus.tinyengine.support.auth;

import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SystemTinyEngineUserSystemService implements TinyEngineUserSystemService {

	private final TinyEngineUserContext userContext;

	public SystemTinyEngineUserSystemService(TinyEngineUserContext userContext) {
		this.userContext = Objects.requireNonNull(userContext, "userContext");
	}

	@Override
	public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
		throw unsupported("login");
	}

	@Override
	public TinyEnginePrincipal currentUser(TinyEngineCredential credential) {
		TinyEngineUser user = userContext.getCurrentUser();
		return new TinyEnginePrincipal(user.getUserId(), user.getUserName(), user.getTenantId(), user.getRenterId(),
				user.getPlatformId());
	}

	@Override
	public void logout(TinyEngineCredential credential) {
	}

	@Override
	public TinyEnginePrincipal register(TinyEngineRegistrationRequest request) {
		throw unsupported("account registration");
	}

	@Override
	public void requestPasswordReset(TinyEnginePasswordResetRequest request) {
		throw unsupported("password reset");
	}

	@Override
	public List<TinyEngineTenant> tenants(TinyEnginePrincipal principal) {
		TinyEnginePrincipal current = principal == null ? currentUser(null) : principal;
		return Collections.singletonList(new TinyEngineTenant(current.getTenantId(), current.getTenantId()));
	}

	@Override
	public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
		TinyEnginePrincipal current = principal == null ? currentUser(null) : principal;
		return new TinyEnginePrincipal(current.getUserId(), current.getUserName(), tenantId,
				current.getRenterId(), current.getPlatformId());
	}

	@Override
	public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
		throw unsupported("tenant create");
	}

	@Override
	public void check(TinyEnginePrincipal principal, String permission) {
	}

	private TinyEngineUnsupportedCapabilityException unsupported(String capability) {
		return new TinyEngineUnsupportedCapabilityException("TinyEngine capability is not configured: " + capability);
	}
}
