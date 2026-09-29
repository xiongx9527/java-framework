package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.model.SseSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisServerSseRegistryTest {
	private static final String SERVER_ID = "server-1";

	private McpRedisBroadcaster broadcaster;

	private RedisServerSseRegistry registry;

	private StringWriter output;

	@BeforeEach
	void setUp() {
		McpServer server = mock(McpServer.class);
		broadcaster = mock(McpRedisBroadcaster.class);
		when(broadcaster.getServerId()).thenReturn(SERVER_ID);
		registry = new RedisServerSseRegistry(server, broadcaster);
		output = new StringWriter();
	}

	@Test
	void shouldSendLocallyWhenSessionExists() {
		registerSession("session-1");

		registry.sendMsg("session-1", "hello");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hello\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldBroadcastToClusterWhenSessionIsMissing() {
		registry.sendMsg("ghost", "hello");

		assertThat(output.toString()).isEmpty();
		verify(broadcaster).broadcast("ghost", "hello", "default");
	}

	@Test
	void shouldBroadcastLocallyAndToCluster() {
		registerSession("session-1");

		registry.broadcastMsg("hi");

		assertThat(output.toString()).isEqualTo("data: hi\n\n");
		verify(broadcaster).broadcast(null, "hi", "default");
	}

	@Test
	void shouldSendOtherLocallyWhenSessionExists() {
		registerSession("session-1");

		registry.sendOther("server-2", "session-1", "hi");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hi\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldSkipSendOtherWhenSessionIsMissing() {
		registry.sendOther("server-2", "ghost", "hi");

		assertThat(output.toString()).isEmpty();
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldNotSendOtherMessagesFromSelf() {
		registerSession("session-1");

		registry.sendOther(SERVER_ID, "session-1", "hi");

		assertThat(output.toString()).isEmpty();
	}

	@Test
	void shouldBroadcastOtherLocallyWithoutRepublishing() {
		registerSession("session-1");

		registry.broadcastOther("server-2", "hi");

		assertThat(output.toString()).isEqualTo("data: hi\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldNotBroadcastOtherMessagesFromSelf() {
		registerSession("session-1");

		registry.broadcastOther(SERVER_ID, "hi");

		assertThat(output.toString()).isEmpty();
	}

	@Test
	void shouldReportLocalSessionPresence() {
		registerSession("session-1");

		assertThat(registry.hasSession("session-1")).isTrue();
		assertThat(registry.hasSession("ghost")).isFalse();
	}

	private void registerSession(String sessionId) {
		registry.addConnections(sessionId, new PrintWriter(output));
	}
}
