package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenant;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantCreateRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineUserMvcControllerTest {

	private final RecordingUserSystemService service = new RecordingUserSystemService();
	private final MockMvc mvc = MockMvcBuilders
			.standaloneSetup(new TinyEngineUserMvcController(service, new TinyEngineCredentialExtractor()))
			.build();

	@Test
	void loginUsesProviderBackedUserSystem() throws Exception {
		mvc.perform(post("/platform-center/api/user/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"alice\",\"password\":\"pwd\",\"tenantId\":\"tenant-1\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.accessToken", is("token-alice")));
	}

	@Test
	void currentUserRequiresBearerToken() throws Exception {
		mvc.perform(get("/platform-center/api/user/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success", is(false)));
	}

	@Test
	void currentUserAndTenantListUseBearerToken() throws Exception {
		mvc.perform(get("/platform-center/api/user/me")
						.header("Authorization", "Bearer token-1")
						.header("X-Tenant-Id", "tenant-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userId", is("u1")))
				.andExpect(jsonPath("$.data.tenantId", is("tenant-1")));

		mvc.perform(get("/platform-center/api/tenant/list")
						.header("Authorization", "Bearer token-1")
						.header("X-Tenant-Id", "tenant-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].tenantId", is("tenant-1")));
	}

	static class RecordingUserSystemService implements TinyEngineUserSystemService {

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
