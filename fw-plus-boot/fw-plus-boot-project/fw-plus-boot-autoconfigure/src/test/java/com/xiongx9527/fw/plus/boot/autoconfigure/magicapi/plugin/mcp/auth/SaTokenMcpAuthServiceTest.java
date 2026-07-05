package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaTokenMcpAuthServiceTest {

	@Test
	void shouldRejectMissingBearerTokenWhenAuthEnabled() {
		SaTokenMcpAuthService authService = new SaTokenMcpAuthService(new MagicAPIMcpProperties(),
				token -> null);

		assertThatThrownBy(() -> authService.authenticate(new MockHttpServletRequest()))
				.isInstanceOf(McpAuthorizationException.class)
				.extracting("status")
				.isEqualTo(401);
	}

	@Test
	void shouldAllowAnonymousWhenAuthDisabled() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getAuth().setEnabled(false);
		SaTokenMcpAuthService authService = new SaTokenMcpAuthService(properties, token -> null);

		McpPrincipal principal = authService.authenticate(new MockHttpServletRequest());

		assertThat(principal.getSubject()).isEqualTo("anonymous");
	}

	@Test
	void shouldValidateBearerToken() {
		SaTokenMcpAuthService authService = new SaTokenMcpAuthService(new MagicAPIMcpProperties(),
				token -> new McpPrincipal("user-1", Collections.singleton("mcp:tools")));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token-1");

		McpPrincipal principal = authService.authenticate(request);

		assertThat(principal.getSubject()).isEqualTo("user-1");
	}

	@Test
	void shouldRejectMissingScope() {
		SaTokenMcpAuthService authService = new SaTokenMcpAuthService(new MagicAPIMcpProperties(),
				token -> new McpPrincipal("user-1", Collections.singleton("mcp:tools")));

		assertThatThrownBy(() -> authService.requireScope(authService.verifyToken("token-1"), "mcp:resources"))
				.isInstanceOf(McpAuthorizationException.class)
				.extracting("status")
				.isEqualTo(403);
	}
}
