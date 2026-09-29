package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpPrincipalTest {

	@Test
	void shouldExposeSubjectAndScopes() {
		Set<String> scopes = new HashSet<>();
		scopes.add("tools:read");

		McpPrincipal principal = new McpPrincipal("user-1", scopes);

		assertThat(principal.getSubject()).isEqualTo("user-1");
		assertThat(principal.getScopes()).containsExactly("tools:read");
	}

	@Test
	void shouldProtectAgainstMutationButAcceptNullScope() {
		Set<String> scopes = new HashSet<>();
		scopes.add("a");

		McpPrincipal principal = new McpPrincipal("user-1", scopes);

		assertThatThrownBy(() -> principal.getScopes().add("b"))
				.isInstanceOf(UnsupportedOperationException.class);
		// null scopes 视为空集合
		assertThat(new McpPrincipal("user-1", null).getScopes()).isEmpty();
	}

	@Test
	void anonymousPrincipalExists() {
		assertThat(McpPrincipal.ANONYMOUS.getSubject()).isEqualTo("anonymous");
		assertThat(McpPrincipal.ANONYMOUS.getScopes()).isEmpty();
	}
}