package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenOAuth2McpAuthServiceTest {

	private final SaTokenOAuth2McpAuthService service = new SaTokenOAuth2McpAuthService();

	@Test
	void shouldRejectWhenAuthorizationHeaderMissing() {
		assertThat(service.authenticate(request(null))).isSameAs(McpPrincipal.ANONYMOUS);
	}

	@Test
	void shouldRejectWhenAuthorizationIsNotBearer() {
		assertThat(service.authenticate(request("Basic abc"))).isSameAs(McpPrincipal.ANONYMOUS);
	}

	@Test
	void shouldRejectWhenBearerTokenBlank() {
		assertThat(service.authenticate(request("Bearer    "))).isSameAs(McpPrincipal.ANONYMOUS);
	}

	@Test
	void shouldRejectWhenSaTokenRejectsToken() {
		// 未初始化的 Sa-Token 上下文下 checkAccessToken 抛异常，转匿名而不是向外抛
		assertThat(service.authenticate(request("Bearer not-a-real-token"))).isSameAs(McpPrincipal.ANONYMOUS);
	}

	private McpRequest request(String authorization) {
		Map<String, String> headers = new HashMap<>();
		if (authorization != null) {
			headers.put("Authorization", authorization);
		}
		return new McpRequest("session-1", "http-sse", "/mcp", "{\"id\":1}", headers);
	}
}
