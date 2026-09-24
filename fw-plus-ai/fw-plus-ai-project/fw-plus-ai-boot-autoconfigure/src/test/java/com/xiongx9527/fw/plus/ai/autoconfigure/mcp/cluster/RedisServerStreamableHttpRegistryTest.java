package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.StreamSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisServerStreamableHttpRegistryTest {
	private static final String SERVER_ID = "server-1";

	private McpRedisBroadcaster broadcaster;

	private RedisServerStreamableHttpRegistry registry;

	private StringWriter output;

	@BeforeEach
	void setUp() {
		McpServer server = mock(McpServer.class);
		broadcaster = mock(McpRedisBroadcaster.class);
		when(broadcaster.getServerId()).thenReturn(SERVER_ID);
		registry = new RedisServerStreamableHttpRegistry(server, broadcaster);
		output = new StringWriter();
	}

	@Test
	void shouldSendLocallyWhenStreamExists() throws Exception {
		registerStream("session-1");

		registry.sendMsg("session-1", "hello");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hello\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldBroadcastToClusterWhenStreamIsMissing() {
		registry.sendMsg("ghost", "hello");

		assertThat(output.toString()).isEmpty();
		verify(broadcaster).broadcast("ghost", "hello", "default");
	}

	@Test
	void shouldBroadcastLocallyAndToCluster() throws Exception {
		registerStream("session-1");

		registry.broadcastMsg("hi");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hi\n\n");
		verify(broadcaster).broadcast(null, "hi", "default");
	}

	@Test
	void shouldSendOtherLocallyWhenStreamExists() throws Exception {
		registerStream("session-1");

		registry.sendOther("server-2", "session-1", "hi");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hi\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldSkipSendOtherWhenStreamIsMissing() {
		registry.sendOther("server-2", "ghost", "hi");

		assertThat(output.toString()).isEmpty();
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldNotSendOtherMessagesFromSelf() throws Exception {
		registerStream("session-1");

		registry.sendOther(SERVER_ID, "session-1", "hi");

		assertThat(output.toString()).isEmpty();
	}

	@Test
	void shouldBroadcastOtherLocallyWithoutRepublishing() throws Exception {
		registerStream("session-1");

		registry.broadcastOther("server-2", "hi");

		assertThat(output.toString()).isEqualTo("event: message\ndata: hi\n\n");
		verify(broadcaster, never()).broadcast(any(), any());
	}

	@Test
	void shouldNotBroadcastOtherMessagesFromSelf() throws Exception {
		registerStream("session-1");

		registry.broadcastOther(SERVER_ID, "hi");

		assertThat(output.toString()).isEmpty();
	}

	@Test
	void shouldReportLocalStreamPresence() throws Exception {
		registerStream("session-1");

		assertThat(registry.hasSession("session-1")).isTrue();
		assertThat(registry.hasSession("ghost")).isFalse();
	}

	@SuppressWarnings("unchecked")
	private void registerStream(String sessionId) throws Exception {
		Field field = ServerStreamableHttp.class.getDeclaredField("streams");
		field.setAccessible(true);
		((Map<String, StreamSession>) field.get(registry)).put(sessionId, new StreamSession(new PrintWriter(output)));
	}
}
