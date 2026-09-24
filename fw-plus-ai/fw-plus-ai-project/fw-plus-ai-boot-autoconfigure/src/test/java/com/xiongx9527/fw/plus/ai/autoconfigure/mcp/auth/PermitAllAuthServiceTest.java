package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermitAllAuthServiceTest {

	private final PermitAllAuthService service = new PermitAllAuthService();

	@Test
	void shouldAlwaysAuthenticateToAnonymous() {
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Bearer evil-token");

		McpPrincipal principal = service.authenticate(
				new McpRequest("session-1", "http-sse", "/mcp", "{\"id\":1}", headers));

		assertThat(principal).isSameAs(McpPrincipal.ANONYMOUS);
		assertThat(principal.getSubject()).isEqualTo("anonymous");
		assertThat(principal.getScopes()).isEmpty();
	}

	@Test
	void shouldAcceptEveryRequestShape() {
		// 空 body / 空 headers / 空 sessionId 均不抛异常
		McpPrincipal principal = service.authenticate(new McpRequest(null, "streamable-http", "/mcp",
				"", Collections.emptyMap()));

		assertThat(principal.getSubject()).isEqualTo("anonymous");
	}
}