package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpSessionServiceTest {

	@Test
	void shouldCreateSessionWithProtocolState() {
		McpSessionService service = new McpSessionService(1800);
		McpPrincipal principal = new McpPrincipal("user-1", Collections.singleton("mcp:tools"));

		McpSession session = service.createSession(principal, Collections.singletonMap("tools", Collections.emptyMap()), "2025-06-18");

		assertThat(session.getId()).isNotBlank();
		assertThat(session.getPrincipal()).isSameAs(principal);
		assertThat(session.getProtocolVersion()).isEqualTo("2025-06-18");
		assertThat(session.getState()).isEqualTo(McpSessionState.INITIALIZED);
		assertThat(service.requireSession(session.getId())).isSameAs(session);
	}

	@Test
	void shouldRejectUnknownSession() {
		McpSessionService service = new McpSessionService(1800);

		assertThatThrownBy(() -> service.requireSession("missing"))
				.isInstanceOf(McpSessionNotFoundException.class);
	}

	@Test
	void shouldNotExpireSessionWhenTimeoutDisabled() {
		McpSessionService service = new McpSessionService(0);
		McpSession session = service.createSession(new McpPrincipal("user-1", Collections.emptySet()),
				Collections.emptyMap(), "2025-06-18");

		assertThat(service.requireSession(session.getId())).isSameAs(session);
	}

	@Test
	void shouldMarkSessionReady() {
		McpSessionService service = new McpSessionService(1800);
		McpSession session = service.createSession(new McpPrincipal("user-1", Collections.emptySet()),
				Collections.emptyMap(), "2025-06-18");

		service.markReady(session.getId());

		assertThat(service.requireSession(session.getId()).getState()).isEqualTo(McpSessionState.READY);
	}
}
