package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisClusterRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;

import java.util.LinkedHashMap;
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

	@Test
	void shouldIgnoreMessagesFromSelf() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisBroadcaster broadcaster = broadcaster();
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster);
		RedisClusterRegistry registry = registry(context);

		consumer.onMessage(messageFor(payload(broadcaster.getServerId(), null, "hello")), new byte[0]);

		verify(registry, never()).broadcastOther(any(), any());
		verify(registry, never()).sendOther(any(), any(), any());
	}

	@Test
	void shouldSendOtherForTargetedMessage() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster());
		RedisClusterRegistry registry = registry(context);

		consumer.onMessage(messageFor(payload("other-node", "session-123", "payload body")), new byte[0]);

		verify(registry).sendOther("other-node", "session-123", "payload body");
	}

	@Test
	void shouldBroadcastOtherWhenSessionIdIsMissing() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster());
		RedisClusterRegistry registry = registry(context);

		consumer.onMessage(messageFor(payload("other-node", null, "payload body")), new byte[0]);

		verify(registry).broadcastOther("other-node", "payload body");
	}

	@Test
	void shouldIgnoreEmptyMessageBody() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster());
		RedisClusterRegistry registry = registry(context);

		consumer.onMessage(messageFor(payload("other-node", null, "")), new byte[0]);

		verify(registry, never()).broadcastOther(any(), any());
	}

	@Test
	void shouldNotPropagateDeliveryFailure() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster());
		RedisClusterRegistry registry = registry(context);
		doThrow(new IllegalStateException("no session")).when(registry).sendOther("other-node", "session-123", "payload body");

		assertThatCode(() -> consumer.onMessage(messageFor(payload("other-node", "session-123", "payload body")), new byte[0]))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldIgnoreMalformedPayload() {
		ApplicationContext context = mock(ApplicationContext.class);
		McpRedisMessageConsumer consumer = new McpRedisMessageConsumer(context, broadcaster());

		Message message = mock(Message.class);
		when(message.getBody()).thenReturn("not-json".getBytes());

		assertThatCode(() -> consumer.onMessage(message, new byte[0])).doesNotThrowAnyException();
		verify(context, never()).getBeanProvider(BaseServerRegistry.class);
	}

	private static McpRedisBroadcaster broadcaster() {
		return new McpRedisBroadcaster(null, "server-1");
	}

	private RedisClusterRegistry registry(ApplicationContext context) {
		RedisClusterRegistry registry = mock(RedisClusterRegistry.class);
		ObjectProvider<BaseServerRegistry> provider = registryProvider(registry);
		when(context.getBeanProvider(BaseServerRegistry.class)).thenReturn(provider);
		return registry;
	}

	private Map<String, Object> payload(String serverId, String sessionId, String message) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("serverId", serverId);
		if (sessionId != null)
			payload.put("sessionId", sessionId);
		payload.put("message", message);
		return payload;
	}

	private Message messageFor(Map<String, Object> payload) throws Exception {
		Message redisMessage = mock(Message.class);
		when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsBytes(payload));
		return redisMessage;
	}

	@SuppressWarnings("unchecked")
	private ObjectProvider<BaseServerRegistry> registryProvider(BaseServerRegistry registry) {
		ObjectProvider<BaseServerRegistry> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(registry);
		return provider;
	}
}
