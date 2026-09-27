package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMcpClusterChangeListenerTest {

	private MagicMcpNotifier notifier;

	private MagicMcpClusterChangeListener listener;

	@BeforeEach
	void setUp() {
		notifier = mock(MagicMcpNotifier.class);
		listener = new MagicMcpClusterChangeListener("node-a", notifier);
	}

	@Test
	void shouldIgnoreMessagesFromSelf() {
		listener.onMessage(message("node-a", "tools"), null);

		verify(notifier, never()).refreshTools();
		verify(notifier, never()).refreshResources();
	}

	@Test
	void shouldRefreshToolsForRemoteToolsChange() {
		listener.onMessage(message("node-b", "tools"), null);

		verify(notifier).refreshTools();
	}

	@Test
	void shouldRefreshResourcesForRemoteResourcesChange() {
		listener.onMessage(message("node-b", "resources"), null);

		verify(notifier).refreshResources();
	}

	@Test
	void shouldRefreshPromptsForRemotePromptsChange() {
		listener.onMessage(message("node-b", "prompts"), null);

		verify(notifier).refreshPrompts();
	}

	@Test
	void shouldIgnoreMessagesOfOtherType() {
		listener.onMessage(raw("{\"serverId\":\"node-b\",\"type\":\"mcp-message\",\"message\":\"{}\"}"), null);

		verify(notifier, never()).refreshTools();
		verify(notifier, never()).refreshResources();
		verify(notifier, never()).refreshPrompts();
	}

	@Test
	void shouldIgnoreUnknownCapability() {
		listener.onMessage(message("node-b", "completion"), null);

		verify(notifier, never()).refreshTools();
		verify(notifier, never()).refreshResources();
		verify(notifier, never()).refreshPrompts();
	}

	private Message raw(String json) {
		return new Message() {
			@Override
			public byte[] getBody() {
				return json.getBytes(StandardCharsets.UTF_8);
			}

			@Override
			public byte[] getChannel() {
				return "fw-plus:mcp:broadcast".getBytes(StandardCharsets.UTF_8);
			}
		};
	}

	private Message message(String source, String capability) {
		String json = "{\"serverId\":\"" + source + "\",\"type\":\"" + MagicMcpClusterChangeListener.MESSAGE_TYPE
				+ "\",\"capability\":\"" + capability + "\"}";
		return raw(json);
	}
}
