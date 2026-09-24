package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * MCP 集群 Redis 消费端，把其它节点发布的消息交给本节点传输投递。
 * <p>
 * 所有节点订阅同一个频道，各自都能收到全部发布消息。{@code serverId} 标识消息来源节点：
 * 与本节点一致的（即本节点自己发布的）消息直接忽略，发布时已完成本地投递。定向消息只
 * 投递给持有该会话的节点，广播消息只做本地分发；两者分别对应集群契约的
 * {@code sendOther} 与 {@code broadcastOther}，由注册表保证不再发布回频道、不在节点间循环。
 * </p>
 */
@Slf4j
public class McpRedisMessageConsumer implements MessageListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApplicationContext context;

    /**
     * 本节点标识，与发布端一致；为 {@code null} 时表示本节点不发布，不会收到自己的消息。
     */
    private final String serverId;

    /**
     * @param context     Spring 应用上下文，用于按需获取当前传输注册表
     * @param broadcaster 集群广播器，提供本节点标识
     */
    public McpRedisMessageConsumer(ApplicationContext context, McpRedisBroadcaster broadcaster) {
        this.context = context;
        this.serverId = broadcaster == null ? null : broadcaster.getServerId();
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        JsonNode payload = parsePayload(message);
        if (payload == null)
            return;

        String source = payload.path("serverId").asText(null);
        if (serverId != null && serverId.equals(source))
            return;

        String body = payload.path("message").asText(null);
        if (body == null || body.isEmpty())
            return;

        String sessionId = payload.path("sessionId").asText(null);
        if (sessionId != null && sessionId.isEmpty())
            sessionId = null;

        deliver(source, sessionId, body);
    }

    private void deliver(String source, String sessionId, String body) {
        try {
            BaseServerRegistry registry = context.getBeanProvider(BaseServerRegistry.class).getIfAvailable();
            if (registry == null)
                return;

            if (sessionId == null) {
                registry.broadcastOther(source, body);
                return;
            }

            // Redis 注册表内部按本机会话持有情况决定投递还是静默跳过，普通注册表只有来源守卫。
            registry.sendOther(source, sessionId, body);
        } catch (RuntimeException e) {
            // 单条消息投递失败不应中断 Redis 监听线程。
            log.warn("Failed to deliver the MCP message from redis: {}", e.getMessage());
        }
    }
    private JsonNode parsePayload(Message message) {
        try {
            return MAPPER.readTree(message.getBody());
        } catch (Exception e) {
            log.warn("Ignoring the malformed MCP message from redis: {}", e.getMessage());
            return null;
        }
    }
}
