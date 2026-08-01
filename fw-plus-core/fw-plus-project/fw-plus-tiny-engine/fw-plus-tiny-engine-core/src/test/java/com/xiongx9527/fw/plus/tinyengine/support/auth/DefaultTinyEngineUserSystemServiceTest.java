package com.xiongx9527.fw.plus.tinyengine.support.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultTinyEngineUserSystemServiceTest {

	private RecordingAuthenticationProvider authenticationProvider;
	private RecordingAccountProvider accountProvider;
	private RecordingTenantProvider tenantProvider;
	private RecordingPermissionEvaluator permissionEvaluator;
	private DefaultTinyEngineUserSystemService service;

	@BeforeEach
	void setUp() {
		authenticationProvider = new RecordingAuthenticationProvider();
		accountProvider = new RecordingAccountProvider();
		tenantProvider = new RecordingTenantProvider();
		permissionEvaluator = new RecordingPermissionEvaluator();
		service = new DefaultTinyEngineUserSystemService(authenticationProvider, accountProvider, tenantProvider,
				permissionEvaluator);
	}

	@Test
	void resolvesPrincipalAndChecksPermission() {
		TinyEngineCredential credential = new TinyEngineCredential("token-1", "tenant-1");
		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", "renter-1",
				Integer.valueOf(1));
		authenticationProvider.principal = principal;

		assertThat(service.currentUser(credential)).isSameAs(principal);
		service.check(principal, TinyEnginePermissions.APP_READ);

		assertThat(authenticationProvider.resolvedCredential).isSameAs(credential);
		assertThat(permissionEvaluator.checkedPrincipal).isSameAs(principal);
		assertThat(permissionEvaluator.checkedPermission).isEqualTo(TinyEnginePermissions.APP_READ);
	}

	@Test
	void delegatesLoginLogoutRegisterPasswordResetAndTenantOperations() {
		TinyEngineLoginRequest loginRequest = new TinyEngineLoginRequest("alice", "pwd", "tenant-1");
		TinyEngineRegistrationRequest registrationRequest =
				new TinyEngineRegistrationRequest("bob", "Bob", "pwd", "tenant-1");
		TinyEnginePasswordResetRequest passwordResetRequest =
				new TinyEnginePasswordResetRequest("bob", "tenant-1", "code-1", "pwd2");
		TinyEngineTenantCreateRequest tenantCreateRequest = new TinyEngineTenantCreateRequest("tenant-2", "Tenant Two");
		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", "renter-1",
				Integer.valueOf(1));

		assertThat(service.login(loginRequest).getAccessToken()).isEqualTo("token-1");
		service.logout(new TinyEngineCredential("token-1", "tenant-1"));
		assertThat(service.register(registrationRequest).getUserId()).isEqualTo("bob");
		service.requestPasswordReset(passwordResetRequest);
		assertThat(service.tenants(principal)).extracting(TinyEngineTenant::getTenantId)
				.containsExactly("tenant-1", "tenant-2");
		assertThat(service.switchTenant(principal, "tenant-2").getTenantId()).isEqualTo("tenant-2");
		assertThat(service.createTenant(principal, tenantCreateRequest).getTenantId()).isEqualTo("tenant-2");

		assertThat(authenticationProvider.loginRequest).isSameAs(loginRequest);
		assertThat(authenticationProvider.logoutCredential.getAccessToken()).isEqualTo("token-1");
		assertThat(accountProvider.registrationRequest).isSameAs(registrationRequest);
		assertThat(accountProvider.passwordResetRequest).isSameAs(passwordResetRequest);
		assertThat(tenantProvider.createdRequest).isSameAs(tenantCreateRequest);
	}

	@Test
	void missingOptionalProvidersReturnUnsupportedCapability() {
		DefaultTinyEngineUserSystemService service = new DefaultTinyEngineUserSystemService(authenticationProvider,
				null, null, permissionEvaluator);
		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", "renter-1",
				Integer.valueOf(1));

		assertThatThrownBy(() -> service.register(new TinyEngineRegistrationRequest("bob", "Bob", "pwd", "tenant-1")))
				.isInstanceOf(TinyEngineUnsupportedCapabilityException.class);
		assertThatThrownBy(() -> service.requestPasswordReset(
				new TinyEnginePasswordResetRequest("bob", "tenant-1", "code-1", "pwd2")))
				.isInstanceOf(TinyEngineUnsupportedCapabilityException.class);
		assertThatThrownBy(() -> service.tenants(principal))
				.isInstanceOf(TinyEngineUnsupportedCapabilityException.class);
		assertThatThrownBy(() -> service.switchTenant(principal, "tenant-2"))
				.isInstanceOf(TinyEngineUnsupportedCapabilityException.class);
		assertThatThrownBy(() -> service.createTenant(principal, new TinyEngineTenantCreateRequest("tenant-2", "Tenant Two")))
				.isInstanceOf(TinyEngineUnsupportedCapabilityException.class);
	}

	@Test
	void rejectsNullPrincipalAndPropagatesPermissionDenial() {
		assertThatThrownBy(() -> service.check(null, TinyEnginePermissions.APP_READ))
				.isInstanceOf(TinyEngineUnauthenticatedException.class);

		permissionEvaluator.deny = true;
		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", "renter-1",
				Integer.valueOf(1));

		assertThatThrownBy(() -> service.check(principal, TinyEnginePermissions.APP_WRITE))
				.isInstanceOf(TinyEngineAccessDeniedException.class)
				.hasMessageContaining(TinyEnginePermissions.APP_WRITE);
	}

	static class RecordingAuthenticationProvider implements TinyEngineAuthenticationProvider {

		private TinyEngineLoginRequest loginRequest;
		private TinyEngineCredential logoutCredential;
		private TinyEngineCredential resolvedCredential;
		private TinyEnginePrincipal principal;

		@Override
		public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
			loginRequest = request;
			return new TinyEngineLoginResult("token-1", "Bearer", Long.valueOf(3600L));
		}

		@Override
		public TinyEnginePrincipal resolve(TinyEngineCredential credential) {
			resolvedCredential = credential;
			return principal;
		}

		@Override
		public void logout(TinyEngineCredential credential) {
			logoutCredential = credential;
		}
	}

	static class RecordingAccountProvider implements TinyEngineAccountProvider {

		private TinyEngineRegistrationRequest registrationRequest;
		private TinyEnginePasswordResetRequest passwordResetRequest;

		@Override
		public TinyEnginePrincipal register(TinyEngineRegistrationRequest request) {
			registrationRequest = request;
			return new TinyEnginePrincipal(request.getUsername(), request.getDisplayName(), request.getTenantId(),
					request.getTenantId(), Integer.valueOf(0));
		}

		@Override
		public void requestPasswordReset(TinyEnginePasswordResetRequest request) {
			passwordResetRequest = request;
		}
	}

	static class RecordingTenantProvider implements TinyEngineTenantProvider {

		private TinyEngineTenantCreateRequest createdRequest;

		@Override
		public List<TinyEngineTenant> listTenants(TinyEnginePrincipal principal) {
			return Arrays.asList(new TinyEngineTenant("tenant-1", "Tenant One"),
					new TinyEngineTenant("tenant-2", "Tenant Two"));
		}

		@Override
		public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
			return new TinyEnginePrincipal(principal.getUserId(), principal.getUserName(), tenantId,
					principal.getRenterId(), principal.getPlatformId());
		}

		@Override
		public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
			createdRequest = request;
			return new TinyEngineTenant(request.getTenantId(), request.getTenantName());
		}
	}

	static class RecordingPermissionEvaluator implements TinyEnginePermissionEvaluator {

		private TinyEnginePrincipal checkedPrincipal;
		private String checkedPermission;
		private boolean deny;

		@Override
		public void check(TinyEnginePrincipal principal, String permission) {
			checkedPrincipal = principal;
			checkedPermission = permission;
			if (deny) {
				throw new TinyEngineAccessDeniedException("Denied permission: " + permission);
			}
		}
	}
}
