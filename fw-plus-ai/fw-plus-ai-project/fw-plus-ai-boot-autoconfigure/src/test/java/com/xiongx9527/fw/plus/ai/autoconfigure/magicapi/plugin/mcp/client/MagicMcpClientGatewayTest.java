package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.client;

import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams.Capabilities;
import com.ajaxjs.mcp.server.McpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMcpClientGatewayTest {

	private McpServer server;

	private MagicMcpClientGateway gateway;

	@BeforeEach
	void setUp() {
		server = mock(McpServer.class);
		gateway = new MagicMcpClientGateway(server);

		Map<String, Capabilities> capabilities = new HashMap<>();
		capabilities.put("session-roots", capabilities("roots", null, null));
		capabilities.put("session-sampling", capabilities(null, "sampling", null));
		capabilities.put("session-elicit", capabilities(null, null, "elicitation"));
		capabilities.put("session-none", new Capabilities());
		when(server.getClientCapabilities()).thenReturn(capabilities);
	}

	@Test
	void shouldReportDeclaredCapabilities() {
		assertThat(gateway.hasCapability("session-roots", "roots")).isTrue();
		assertThat(gateway.hasCapability("session-roots", "sampling")).isFalse();
		assertThat(gateway.hasCapability("session-sampling", "sampling")).isTrue();
		assertThat(gateway.hasCapability("session-elicit", "elicitation")).isTrue();
		assertThat(gateway.hasCapability("session-none", "roots")).isFalse();
		assertThat(gateway.hasCapability("unknown-session", "roots")).isFalse();
	}

	@Test
	void shouldNotAskForRootsWhenCapabilityMissing() {
		assertThat(gateway.listRoots("session-none")).isEmpty();
		assertThat(gateway.listRoots("unknown-session")).isEmpty();

		verify(server, never()).listRoots(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(java.time.Duration.class));
	}

	@Test
	void shouldAskForRootsWhenCapabilityPresent() {
		gateway.listRoots("session-roots");

		verify(server).listRoots("session-roots", java.time.Duration.ofSeconds(30));
	}

	@Test
	void shouldRejectSamplingWithoutCapability() {
		assertThatThrownBy(() -> gateway.createMessage("session-none", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sampling");

		verify(server, never()).createMessage(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(java.time.Duration.class));
	}

	@Test
	void shouldRejectElicitWithoutCapability() {
		assertThatThrownBy(() -> gateway.elicit("session-roots", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("elicitation");
	}

	@Test
	void shouldForwardSamplingWhenCapabilityPresent() {
		gateway.createMessage("session-sampling", null);

		verify(server).createMessage("session-sampling", null, java.time.Duration.ofSeconds(30));
	}

	@Test
	void shouldExposePendingRequestView() {
		when(server.getRunningRequests()).thenReturn(new HashMap<>());
		when(server.getPendingClientResponses()).thenReturn(new HashMap<>());

		assertThat(gateway.runningRequests()).isEmpty();
		assertThat(gateway.pendingClientResponses()).isEmpty();
	}

	private Capabilities capabilities(String roots, String sampling, String elicitation) {
		Capabilities capabilities = new Capabilities();
		if (roots != null) {
			capabilities.setRoots(new com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams.Capabilities.Roots());
		}
		if (sampling != null) {
			capabilities.setSampling(
					new com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams.Capabilities.Sampling());
		}
		if (elicitation != null) {
			capabilities.setElicitation(
					new com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams.Capabilities.Elicitation());
		}
		return capabilities;
	}
}
