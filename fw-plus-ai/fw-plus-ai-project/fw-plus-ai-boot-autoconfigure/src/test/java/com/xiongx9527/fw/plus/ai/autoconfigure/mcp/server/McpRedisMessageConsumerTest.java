package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpRedisMessageConsumerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final String SELF_NODE = "server-1";

	@Test
	void shouldIgnoreMessagesFromSelf() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);

		consumer.onMessage(messageFor(payload(SELF_NODE, null, "hello", null)), new byte[0]);

		verify(registry, never()).broadcastOther(any(), any());
		verify(registry, never()).sendOther(any(), any(), any());
	}

	@Test
	void shouldSendOtherForTargetedMessage() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);

		consumer.onMessage(messageFor(payload("other-node", "session-123", "payload body", null)), new byte[0]);

		verify(registry).sendOther("other-node", "session-123", "payload body");
	}

	@Test
	void shouldBroadcastOtherWhenSessionIdIsMissing() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);

		consumer.onMessage(messageFor(payload("other-node", null, "payload body", null)), new byte[0]);

		verify(registry).broadcastOther("other-node", "payload body");
	}

	@Test
	void shouldIgnoreEmptyMessageBody() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);

		consumer.onMessage(messageFor(payload("other-node", null, "", null)), new byte[0]);

		verify(registry, never()).broadcastOther(any(), any());
	}

	@Test
	void shouldDeliverToAllRegistriesWhenRouteKeyIsMissing() throws Exception {
		BaseServerRegistry defaultRegistry = registry();
		BaseServerRegistry weatherRegistry = registry();
		McpRedisMessageConsumer consumer = consumer(
				registryRoute("default", defaultRegistry),
				registryRoute("weather", weatherRegistry));

		consumer.onMessage(messageFor(payload("other-node", null, "data", null)), new byte[0]);

		verify(defaultRegistry).broadcastOther("other-node", "data");
		verify(weatherRegistry).broadcastOther("other-node", "data");
	}

	@Test
	void shouldRouteByRouteKeyToSingleRuntime() throws Exception {
		BaseServerRegistry defaultRegistry = registry();
		BaseServerRegistry weatherRegistry = registry();
		McpRedisMessageConsumer consumer = consumer(
				registryRoute("default", defaultRegistry),
				registryRoute("weather", weatherRegistry));

		consumer.onMessage(messageFor(payload("other-node", null, "weather data", "weather")), new byte[0]);

		verify(defaultRegistry, never()).broadcastOther(any(), any());
		verify(weatherRegistry).broadcastOther("other-node", "weather data");
	}

	@Test
	void shouldNotDeliverForUnknownRouteKey() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);

		consumer.onMessage(messageFor(payload("other-node", null, "data", "no-such-key")), new byte[0]);

		verify(registry, never()).broadcastOther(any(), any());
	}

	@Test
	void shouldNotPropagateDeliveryFailure() throws Exception {
		BaseServerRegistry registry = registry();
		McpRedisMessageConsumer consumer = consumer("default", registry);
		doThrow(new IllegalStateException("no session")).when(registry).sendOther("other-node", "session-123", "payload body");

		assertThatCode(() -> consumer.onMessage(messageFor(payload("other-node", "session-123", "payload body", null)), new byte[0]))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldIgnoreMalformedPayload() {
		McpRedisMessageConsumer consumer = consumer("default", registry());

		Message message = mock(Message.class);
		when(message.getBody()).thenReturn("not-json".getBytes());

		assertThatCode(() -> consumer.onMessage(message, new byte[0])).doesNotThrowAnyException();
	}

	private BaseServerRegistry registry() {
		return mock(BaseServerRegistry.class);
	}

	private static final class RegistryRoute {

		private final String key;

		private final BaseServerRegistry registry;

		RegistryRoute(String key, BaseServerRegistry registry) {
			this.key = key;
			this.registry = registry;
		}
	}

	private RegistryRoute registryRoute(String key, BaseServerRegistry registry) {
		return new RegistryRoute(key, registry);
	}

	private McpRedisMessageConsumer consumer(String key, BaseServerRegistry registry) {
		return consumer(registryRoute(key, registry));
	}

	private McpRedisMessageConsumer consumer(RegistryRoute... routes) {
		McpRedisBroadcaster broadcaster = new McpRedisBroadcaster(null, SELF_NODE);
		List<McpServerRuntime> runtimes = new ArrayList<>();
		for (RegistryRoute route : routes) {
			runtimes.add(new McpServerRuntime(route.key, null, null, null, route.registry, null,
					Collections.emptyList()));
		}
		return new McpRedisMessageConsumer(runtimes, broadcaster);
	}

	private Map<String, Object> payload(String serverId, String sessionId, String message, String routeKey) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("serverId", serverId);
		if (sessionId != null)
			payload.put("sessionId", sessionId);
		payload.put("message", message);
		if (routeKey != null)
			payload.put("routeKey", routeKey);
		return payload;
	}

	private Message messageFor(Map<String, Object> payload) throws Exception {
		Message redisMessage = mock(Message.class);
		when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsBytes(payload));
		return redisMessage;
	}
}