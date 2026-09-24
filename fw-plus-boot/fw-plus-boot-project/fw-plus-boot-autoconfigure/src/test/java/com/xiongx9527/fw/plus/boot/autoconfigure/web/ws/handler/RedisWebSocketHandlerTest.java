package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Redis 跨实例广播机制的验收测试（不依赖真实 Redis 实例）。
 *
 * @author zzy
 */
class RedisWebSocketHandlerTest {

    @Test
    void distributedBroadcastShouldPublishToConfiguredChannel() throws Exception {
        WsConfig wsConfig = new WsConfig();
        wsConfig.getRedis().getChannel().setBroadcast("custom:ws:broadcast");
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisWebSocketHandler handler = new RedisWebSocketHandler(wsConfig);
        injectRedisTemplate(handler, redisTemplate);

        handler.broadcastDistributedMessageToAllClient("app-a", "hello");

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), payloadCaptor.capture());
        assertThat(channelCaptor.getValue()).isEqualTo("custom:ws:broadcast");

        JSONObject payload = JSON.parseObject(payloadCaptor.getValue());
        assertThat(payload.getString("appId")).isEqualTo("app-a");
        assertThat(payload.getString("message")).isEqualTo("hello");
        assertThat(payload.getString("clientId")).isNull();
        assertThat(payload.getString("excludedServerId")).isEqualTo(handler.getWebSocketServerId());
    }

    @Test
    void distributedBroadcastShouldDefaultToFrozenChannelName() throws Exception {
        RedisWebSocketHandler handler = new RedisWebSocketHandler(new WsConfig());
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        injectRedisTemplate(handler, redisTemplate);

        handler.broadcastDistributedMessageToAllClient("app-a", "hello");

        verify(redisTemplate).convertAndSend(anyString(), anyString());
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), anyString());
        assertThat(channelCaptor.getValue()).isEqualTo("fw-plus:websocket:broadcast");
    }

    @Test
    void listenerShouldDeliverRemoteBroadcastLocally() {
        RecordingHandler handler = new RecordingHandler(new WsConfig());
        String body = "{\"excludedServerId\":\"other-server\",\"appId\":\"app-a\","
                + "\"clientId\":\"client-a\",\"message\":\"hi\"}";

        handler.getRedisMessageListener().onMessage(message(body), null);

        assertThat(handler.clientBroadcasts).containsExactly("app-a|client-a|hi");
        assertThat(handler.allBroadcasts).isEmpty();
    }

    @Test
    void listenerShouldSkipOwnServerAndMalformedMessages() {
        RecordingHandler handler = new RecordingHandler(new WsConfig());
        String ownServer = "{\"excludedServerId\":\"" + handler.getWebSocketServerId() + "\",\"appId\":\"app-a\","
                + "\"clientId\":\"client-a\",\"message\":\"hi\"}";
        String nullAppId = "{\"excludedServerId\":\"other-server\",\"clientId\":\"client-a\",\"message\":\"hi\"}";

        handler.getRedisMessageListener().onMessage(message(ownServer), null);
        handler.getRedisMessageListener().onMessage(message(nullAppId), null);

        assertThat(handler.clientBroadcasts).isEmpty();
        assertThat(handler.allBroadcasts).isEmpty();
    }

    private static Message message(String body) {
        return new DefaultMessage("fw-plus:websocket:broadcast".getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8));
    }

    private static void injectRedisTemplate(RedisWebSocketHandler handler, StringRedisTemplate template)
            throws Exception {
        Field field = RedisWebSocketHandler.class.getDeclaredField("stringRedisTemplate");
        field.setAccessible(true);
        field.set(handler, template);
    }

    /**
     * 记录本地投递调用，用于断言监听器的行为。
     */
    private static class RecordingHandler extends RedisWebSocketHandler {

        private final List<String> clientBroadcasts = new ArrayList<>();

        private final List<String> allBroadcasts = new ArrayList<>();

        RecordingHandler(WsConfig wsConfig) {
            super(wsConfig);
        }

        @Override
        public boolean broadcastMessageToClientLocalOnly(String appId, String clientId, String message) {
            clientBroadcasts.add(appId + "|" + clientId + "|" + message);
            return true;
        }

        @Override
        public void broadcastMessageToAllClientLocalOnly(String appId, String message) {
            allBroadcasts.add(appId + "|" + message);
        }
    }
}
