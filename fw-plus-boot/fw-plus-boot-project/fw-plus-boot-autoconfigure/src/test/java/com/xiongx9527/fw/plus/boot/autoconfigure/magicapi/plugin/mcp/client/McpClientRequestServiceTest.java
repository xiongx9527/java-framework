package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpClientRequestServiceTest {

	@Test
	void shouldCreateRootsListRequestWhenClientSupportsRoots() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpSession session = sessionService.createSession(null, capabilities("roots"), "2025-06-18");
		McpClientRequestService service = new McpClientRequestService(sessionService);

		Map<String, Object> request = service.rootsList(session.getId());

		assertThat(request.get("method")).isEqualTo("roots/list");
		assertThat(session.getClientRequests()).containsKey(request.get("id"));
		assertThat(session.getPendingClientRequests()).containsKey(request.get("id"));
	}

	@Test
	void shouldResolvePendingClientRequestResult() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpSession session = sessionService.createSession(null, capabilities("roots"), "2025-11-25");
		McpClientRequestService service = new McpClientRequestService(sessionService);
		Map<String, Object> request = service.rootsList(session.getId());

		service.resolve(session.getId(), request.get("id"), Collections.singletonMap("roots", Collections.emptyList()), null);

		McpClientRequestRecord record = session.getPendingClientRequests().get(request.get("id"));
		assertThat(record.getStatus()).isEqualTo(McpClientRequestStatus.RESOLVED);
		assertThat(record.getResult()).isInstanceOf(Map.class);
	}

	@Test
	void shouldMarkPendingClientRequestTimedOut() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpSession session = sessionService.createSession(null, capabilities("roots"), "2025-11-25");
		McpClientRequestService service = new McpClientRequestService(sessionService, 0);
		Map<String, Object> request = service.rootsList(session.getId());

		service.cleanupTimedOut(session.getId(), System.currentTimeMillis() + 1);

		McpClientRequestRecord record = session.getPendingClientRequests().get(request.get("id"));
		assertThat(record.getStatus()).isEqualTo(McpClientRequestStatus.TIMED_OUT);
	}

	@Test
	void shouldRejectUnsupportedSamplingCapability() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpSession session = sessionService.createSession(null, Collections.emptyMap(), "2025-06-18");
		McpClientRequestService service = new McpClientRequestService(sessionService);

		assertThatThrownBy(() -> service.samplingCreateMessage(session.getId(), Collections.emptyMap()))
				.isInstanceOf(McpClientCapabilityException.class)
				.hasMessageContaining("sampling");
	}

	private Map<String, Object> capabilities(String capability) {
		Map<String, Object> capabilities = new HashMap<>();
		capabilities.put(capability, Collections.emptyMap());
		return capabilities;
	}
}
