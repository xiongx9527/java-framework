package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineServletAuthenticationFilterTest {

	@Test
	void appApiRequiresBearerTokenWhenSecurityIsEnabled() throws Exception {
		TinyEngineRequestUserScope userScope = new TinyEngineRequestUserScope();
		TinyEngineUserSystemService userSystemService = mock(TinyEngineUserSystemService.class);
		AppService appService = mock(AppService.class);
		TinyEngineApiFacade facade = new DefaultTinyEngineApiFacade(appService, null, null, null, null,
				null, null, null, null, null, null, null, null, new DefaultTinyEngineExternalCapability());
		TinyEngineServletAuthenticationFilter filter = new TinyEngineServletAuthenticationFilter(
				new TinyEngineCredentialExtractor(), userSystemService, userScope);
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new TinyEngineAppMvcController(facade))
				.addFilters(filter).build();

		mvc.perform(post("/app-center/api/apps/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"created-app\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success", is(false)))
				.andExpect(jsonPath("$.code", is("401")));
		verify(appService, never()).createApp(any(App.class));
		mvc.perform(post("/tiny-engine/app-center/api/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"appId\":2,\"serviceId\":10,\"modelName\":\"test-model\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("401")));

		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "system", "system",
				Integer.valueOf(1));
		when(userSystemService.currentUser(any(TinyEngineCredential.class))).thenReturn(principal);
		when(appService.createApp(any(App.class))).thenAnswer(invocation -> {
			assertThat(userScope.getCurrentUser()).isSameAs(principal);
			App created = invocation.getArgument(0);
			created.setId(Integer.valueOf(9));
			return created;
		});

		mvc.perform(post("/app-center/api/apps/create")
						.header("Authorization", "Bearer token")
						.header("X-Tenant-Id", "system")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"created-app\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(9)));
		verify(appService).createApp(any(App.class));
	}
}
