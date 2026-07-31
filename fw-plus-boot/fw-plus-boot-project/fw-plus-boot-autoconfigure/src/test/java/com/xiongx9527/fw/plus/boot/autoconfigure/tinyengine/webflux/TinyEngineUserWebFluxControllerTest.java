package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenant;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantCreateRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

class TinyEngineUserWebFluxControllerTest {

	private final RecordingUserSystemService service = new RecordingUserSystemService();
	private final TinyEngineRequestUserScope scope = new TinyEngineRequestUserScope();
	private final WebTestClient client = WebTestClient.bindToController(new TinyEngineUserWebFluxController(service,
					new TinyEngineCredentialExtractor(),
					new TinyEngineReactiveExecutor(Schedulers.newSingle("tiny-engine-blocking-test"), Duration.ofSeconds(5)),
					scope))
			.build();

	@Test
	void currentUserRequiresBearerToken() {
		client.get().uri("/platform-center/api/user/me")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.success").isEqualTo(false);
	}

	@Test
	void tenantListRunsThroughReactiveExecutorAndRequestScope() {
		client.get().uri("/platform-center/api/tenant/list")
				.header("Authorization", "Bearer token-1")
				.header("X-Tenant-Id", "tenant-1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data[0].tenantId").isEqualTo("tenant-1");
		org.assertj.core.api.Assertions.assertThat(service.lastTenantThread).startsWith("tiny-engine-blocking-test");
		org.assertj.core.api.Assertions.assertThat(service.lastScopedUserId).isEqualTo("u1");
	}

	@Test
	void loginUsesSameJsonContractAsMvc() {
		client.post().uri("/platform-center/api/user/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"username\":\"alice\",\"password\":\"pwd\",\"tenantId\":\"tenant-1\"}")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.success").isEqualTo(true)
				.jsonPath("$.data.accessToken").isEqualTo("token-alice");
	}

	class RecordingUserSystemService implements TinyEngineUserSystemService {

		private String lastTenantThread;
		private String lastScopedUserId;

		@Override
		public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
			return new TinyEngineLoginResult("token-" + request.getUsername(), "Bearer", Long.valueOf(3600L));
		}

		@Override
		public TinyEnginePrincipal currentUser(TinyEngineCredential credential) {
			return new TinyEnginePrincipal("u1", "User One", credential.getTenantId(), "r1", Integer.valueOf(1));
		}

		@Override
		public void logout(TinyEngineCredential credential) {
		}

		@Override
		public TinyEnginePrincipal register(com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineRegistrationRequest request) {
			return new TinyEnginePrincipal(request.getUsername(), request.getDisplayName(), request.getTenantId(), "r1",
					Integer.valueOf(1));
		}

		@Override
		public void requestPasswordReset(com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePasswordResetRequest request) {
		}

		@Override
		public List<TinyEngineTenant> tenants(TinyEnginePrincipal principal) {
			lastTenantThread = Thread.currentThread().getName();
			lastScopedUserId = scope.getCurrentUser().getUserId();
			return Arrays.asList(new TinyEngineTenant(principal.getTenantId(), "Tenant One"));
		}

		@Override
		public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
			return new TinyEnginePrincipal(principal.getUserId(), principal.getUserName(), tenantId,
					principal.getRenterId(), principal.getPlatformId());
		}

		@Override
		public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
			return new TinyEngineTenant(request.getTenantId(), request.getTenantName());
		}

		@Override
		public void check(TinyEnginePrincipal principal, String permission) {
		}
	}
}
