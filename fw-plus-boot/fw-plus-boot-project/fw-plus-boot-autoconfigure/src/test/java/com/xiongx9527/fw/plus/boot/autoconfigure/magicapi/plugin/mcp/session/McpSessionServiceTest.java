package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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

	@Test
	void shouldUseRepositoryForSessionLifecycle() {
		RecordingMcpSessionRepository repository = new RecordingMcpSessionRepository();
		McpSessionService service = new McpSessionService(repository, 1800);

		McpSession session = service.createSession(new McpPrincipal("user-1", Collections.emptySet()),
				Collections.emptyMap(), "2025-06-18");
		service.requireSession(session.getId());
		service.delete(session.getId());

		assertThat(repository.savedSessionId).isEqualTo(session.getId());
		assertThat(repository.touchedSessionId).isEqualTo(session.getId());
		assertThat(repository.deletedSessionId).isEqualTo(session.getId());
	}

	private static class RecordingMcpSessionRepository implements McpSessionRepository {

		private final Map<String, McpSession> saved = new LinkedHashMap<>();

		private String savedSessionId;

		private String touchedSessionId;

		private String deletedSessionId;

		@Override
		public void save(McpSession session) {
			savedSessionId = session.getId();
			saved.put(session.getId(), session);
		}

		@Override
		public McpSession findById(String sessionId) {
			return saved.get(sessionId);
		}

		@Override
		public void touch(String sessionId, long lastActivityAt) {
			touchedSessionId = sessionId;
			McpSession session = saved.get(sessionId);
			if (session != null) {
				session.setLastActivityAt(lastActivityAt);
			}
		}

		@Override
		public void delete(String sessionId) {
			deletedSessionId = sessionId;
			saved.remove(sessionId);
		}

		@Override
		public Collection<McpSession> findAll() {
			return saved.values();
		}
	}
}
