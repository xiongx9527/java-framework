package com.xiongx9527.fw.plus.tinyengine.support.auth;

import java.util.List;
import java.util.Objects;

public class DefaultTinyEngineUserSystemService implements TinyEngineUserSystemService {

	private final TinyEngineAuthenticationProvider authenticationProvider;
	private final TinyEngineAccountProvider accountProvider;
	private final TinyEngineTenantProvider tenantProvider;
	private final TinyEnginePermissionEvaluator permissionEvaluator;

	public DefaultTinyEngineUserSystemService(TinyEngineAuthenticationProvider authenticationProvider,
			TinyEnginePermissionEvaluator permissionEvaluator) {
		this(authenticationProvider, null, null, permissionEvaluator);
	}

	public DefaultTinyEngineUserSystemService(TinyEngineAuthenticationProvider authenticationProvider,
			TinyEngineAccountProvider accountProvider, TinyEngineTenantProvider tenantProvider,
			TinyEnginePermissionEvaluator permissionEvaluator) {
		this.authenticationProvider = Objects.requireNonNull(authenticationProvider, "authenticationProvider");
		this.accountProvider = accountProvider;
		this.tenantProvider = tenantProvider;
		this.permissionEvaluator = Objects.requireNonNull(permissionEvaluator, "permissionEvaluator");
	}

	@Override
	public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
		return authenticationProvider.login(Objects.requireNonNull(request, "request"));
	}

	@Override
	public TinyEnginePrincipal currentUser(TinyEngineCredential credential) {
		TinyEnginePrincipal principal = authenticationProvider.resolve(Objects.requireNonNull(credential, "credential"));
		if (principal == null) {
			throw new TinyEngineUnauthenticatedException("TinyEngine principal is not authenticated");
		}
		return principal;
	}

	@Override
	public void logout(TinyEngineCredential credential) {
		authenticationProvider.logout(Objects.requireNonNull(credential, "credential"));
	}

	@Override
	public TinyEnginePrincipal register(TinyEngineRegistrationRequest request) {
		if (accountProvider == null) {
			throw unsupported("account registration");
		}
		return accountProvider.register(Objects.requireNonNull(request, "request"));
	}

	@Override
	public void requestPasswordReset(TinyEnginePasswordResetRequest request) {
		if (accountProvider == null) {
			throw unsupported("password reset");
		}
		accountProvider.requestPasswordReset(Objects.requireNonNull(request, "request"));
	}

	@Override
	public List<TinyEngineTenant> tenants(TinyEnginePrincipal principal) {
		if (tenantProvider == null) {
			throw unsupported("tenant list");
		}
		return tenantProvider.listTenants(requirePrincipal(principal));
	}

	@Override
	public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
		if (tenantProvider == null) {
			throw unsupported("tenant switch");
		}
		return tenantProvider.switchTenant(requirePrincipal(principal), Objects.requireNonNull(tenantId, "tenantId"));
	}

	@Override
	public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
		if (tenantProvider == null) {
			throw unsupported("tenant create");
		}
		return tenantProvider.createTenant(requirePrincipal(principal), Objects.requireNonNull(request, "request"));
	}

	@Override
	public void check(TinyEnginePrincipal principal, String permission) {
		permissionEvaluator.check(requirePrincipal(principal), Objects.requireNonNull(permission, "permission"));
	}

	private TinyEnginePrincipal requirePrincipal(TinyEnginePrincipal principal) {
		if (principal == null) {
			throw new TinyEngineUnauthenticatedException("TinyEngine principal is required");
		}
		return principal;
	}

	private TinyEngineUnsupportedCapabilityException unsupported(String capability) {
		return new TinyEngineUnsupportedCapabilityException("TinyEngine capability is not configured: " + capability);
	}
}
