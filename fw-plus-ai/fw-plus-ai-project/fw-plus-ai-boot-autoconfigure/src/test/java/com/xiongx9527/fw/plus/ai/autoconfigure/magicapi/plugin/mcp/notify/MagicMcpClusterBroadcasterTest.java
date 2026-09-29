package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMcpClusterBroadcasterTest {

	@Test
	void shouldPublishCapabilityChangeWithOrigin() {
		McpRedisBroadcaster broadcaster = mock(McpRedisBroadcaster.class);
		when(broadcaster.getServerId()).thenReturn("node-a");
		MagicMcpClusterBroadcaster cluster = new MagicMcpClusterBroadcaster(broadcaster);

		cluster.onApplicationEvent(new MagicMcpCapabilityChangedEvent("prompts"));

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(broadcaster).broadcast(eq(null), payload.capture());
		assertThat(payload.getValue())
				.contains("\"type\":\"" + MagicMcpClusterChangeListener.MESSAGE_TYPE + "\"")
				.contains("\"serverId\":\"node-a\"")
				.contains("\"capability\":\"prompts\"");
	}
}
