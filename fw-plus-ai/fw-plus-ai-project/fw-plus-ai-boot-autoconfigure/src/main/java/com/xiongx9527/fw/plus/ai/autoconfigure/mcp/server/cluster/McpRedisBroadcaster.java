package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 集群广播器，把本节点的消息经 Redis 频道投递给集群内其它节点。
 * <p>
 * 同一 Redis 上的 MCP 节点共用一个频道 {@value #CHANNEL}；{@code serverId} 在节点启动时随机生成，
 * 随消息发布出去，仅供消费端识别并忽略本节点自己发布的消息，避免同一份消息被投递两次。
 * 跨节点定向发送由调用方传入目标 {@code sessionId}，由拥有该会话的节点投递。
 * </p>
 */
public class McpRedisBroadcaster {

    /**
     * 集群广播频道名。
     */
    public static final String CHANNEL = "fw-plus:mcp:broadcast";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    /**
     * 本节点标识，随机生成。
     */
    private final String serverId;

    /**
     * 使用随机 {@code serverId} 构造广播器。
     *
     * @param redisTemplate Redis 模板
     */
    public McpRedisBroadcaster(StringRedisTemplate redisTemplate) {
        this(redisTemplate, UUID.randomUUID().toString());
    }

    /**
     * @param redisTemplate Redis 模板
     * @param serverId      本节点标识，测试或需要可预测标识时显式指定
     */
    public McpRedisBroadcaster(StringRedisTemplate redisTemplate, String serverId) {
        this.redisTemplate = redisTemplate;
        this.serverId = serverId;
    }

    /**
     * 返回本节点标识。
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * 发布一条 MCP 消息到集群频道。
     *
     * @param sessionId 目标会话 id，为空表示广播给集群内全部会话
     * @param message   JSON 消息体
     */
    public void broadcast(String sessionId, String message) {
        broadcast(sessionId, message, null);
    }

    /**
     * 发布一条 MCP 消息到集群频道。
     *
     * @param sessionId 目标会话 id，为空表示广播给集群内全部会话
     * @param message   JSON 消息体
     * @param routeKey  目标 runtime key，消费端据此定位本机对应 registry；为空表示不加区分
     */
    public void broadcast(String sessionId, String message, String routeKey) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("serverId", serverId);
        payload.put("sessionId", sessionId == null ? "" : sessionId);
        payload.put("routeKey", routeKey == null ? "" : routeKey);
        payload.put("message", message);

        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize the MCP broadcast payload", e);
        }

        redisTemplate.convertAndSend(CHANNEL, json);
    }
}
