package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class McpRedisBroadcasterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

	@Test
	void shouldPublishServerIdSessionIdAndMessageToClusterChannel() throws Exception {
		new McpRedisBroadcaster(redisTemplate, "server-1").broadcast("session-9", "{\"jsonrpc\":\"2.0\"}");

		ArgumentCaptor<String> channel = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).convertAndSend(channel.capture(), payload.capture());

		assertThat(channel.getValue()).isEqualTo(McpRedisBroadcaster.CHANNEL);
		JsonNode json = objectMapper.readTree(payload.getValue());
		assertThat(json.path("serverId").asText()).isEqualTo("server-1");
		assertThat(json.path("sessionId").asText()).isEqualTo("session-9");
		assertThat(json.path("routeKey").asText()).isEmpty();
		assertThat(json.path("message").asText()).isEqualTo("{\"jsonrpc\":\"2.0\"}");
	}

	@Test
	void shouldPublishEmptySessionIdForClusterBroadcast() throws Exception {
		new McpRedisBroadcaster(redisTemplate, "server-1").broadcast(null, "data");

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(McpRedisBroadcaster.CHANNEL), payload.capture());

		JsonNode json = objectMapper.readTree(payload.getValue());
		assertThat(json.path("sessionId").asText()).isEmpty();
		assertThat(json.path("message").asText()).isEqualTo("data");
	}

	@Test
	void shouldPublishRouteKeyWhenProvided() throws Exception {
		new McpRedisBroadcaster(redisTemplate, "server-1").broadcast("session-9", "{\"jsonrpc\":\"2.0\"}", "weather");

		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(McpRedisBroadcaster.CHANNEL), payload.capture());

		JsonNode json = objectMapper.readTree(payload.getValue());
		assertThat(json.path("serverId").asText()).isEqualTo("server-1");
		assertThat(json.path("sessionId").asText()).isEqualTo("session-9");
		assertThat(json.path("routeKey").asText()).isEqualTo("weather");
		assertThat(json.path("message").asText()).isEqualTo("{\"jsonrpc\":\"2.0\"}");
	}

	@Test
	void shouldGenerateDistinctRandomServerIds() {
		String first = new McpRedisBroadcaster(redisTemplate).getServerId();
		String second = new McpRedisBroadcaster(redisTemplate).getServerId();

		assertThat(first).isNotBlank();
		assertThat(second).isNotBlank();
		assertThat(first).isNotEqualTo(second);
	}
}
