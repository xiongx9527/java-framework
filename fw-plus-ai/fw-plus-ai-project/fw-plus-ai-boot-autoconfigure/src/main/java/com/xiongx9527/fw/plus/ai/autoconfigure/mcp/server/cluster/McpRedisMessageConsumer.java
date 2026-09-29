package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 集群 Redis 消费端，把其它节点发布的消息按 route key 交给对应 runtime 的传输投递。
 * <p>
 * 所有节点订阅同一个频道，各自都能收到全部发布消息。{@code serverId} 标识消息来源节点：
 * 与本节点一致的（即本节点自己发布的）消息直接忽略，发布时已完成本地投递。定向消息只
 * 投递给持有该会话的节点，广播消息只做本地分发；这两者分别对应集群契约的
 * {@code sendOther} 与 {@code broadcastOther}，由注册表保证不再发布回频道、不在节点间循环。
 * </p>
 *
 * @see McpRoutingRegistrar 同一 runtime 的多种传输注册
 */
@Slf4j
public class McpRedisMessageConsumer implements MessageListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 本节点标识，与发布端一致；为 {@code null} 时表示本节点不发布，不会收到自己的消息。
     */
    private final String serverId;

    private final Map<String, BaseServerRegistry> registriesByKey;

    /**
     * @param runtimes    全部 runtime，按其 key 建立注册表路由表
     * @param broadcaster 集群广播器，提供本节点标识
     */
    public McpRedisMessageConsumer(List<McpServerRuntime> runtimes, McpRedisBroadcaster broadcaster) {
        this.serverId = broadcaster == null ? null : broadcaster.getServerId();
        Map<String, BaseServerRegistry> byKey = new LinkedHashMap<>();
        if (runtimes != null) {
            for (McpServerRuntime runtime : runtimes) {
                byKey.put(runtime.getKey(), runtime.getRegistry());
            }
        }
        this.registriesByKey = byKey;
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

        String routeKey = payload.path("routeKey").asText(null);
        if (routeKey != null && routeKey.isEmpty())
            routeKey = null;

        deliver(source, sessionId, routeKey, body);
    }

    /**
     * 按 route key 目标注册表并投递。
     * <p>
     * route key 为 {@code null} 表示旧协议/广播无目标，投递给同 transport 语义下全部注册表；
     * 结果为 {@code default} 时按 {@code _default}/缺省桶投递。各注册表内部仍按本机会话持有
     * 情况决定投递还是静默跳过，普通注册表只有来源守卫。
     * </p>
     */
    private void deliver(String source, String sessionId, String routeKey, String body) {
        List<BaseServerRegistry> targets = resolveTargets(routeKey);
        for (BaseServerRegistry registry : targets) {
            try {
                if (sessionId == null) {
                    registry.broadcastOther(source, body);
                    continue;
                }
                // Redis 注册表内部按本机会话持有情况决定投递还是静默跳过，普通注册表只有来源守卫。
                registry.sendOther(source, sessionId, body);
            } catch (RuntimeException e) {
                // 单条消息投递失败不应中断 Redis 监听线程。
                log.warn("Failed to deliver the MCP message from redis: {}", e.getMessage());
            }
        }
    }

    private List<BaseServerRegistry> resolveTargets(String routeKey) {
        if (routeKey == null || routeKey.isEmpty()) {
            // 旧协议/广播消息无目标 runtime，投递给全部注册表。
            return new ArrayList<>(registriesByKey.values());
        }

        BaseServerRegistry target = registriesByKey.get(routeKey);
        if (target == null) {
            // 本节点不持有该 key 的 runtime（例如集群分属），静默丢弃，不做二次广播。
            log.debug("Ignoring the MCP message from redis targeting local-unknown server key '{}'", routeKey);
            return new ArrayList<>();
        }
        List<BaseServerRegistry> targets = new ArrayList<>();
        targets.add(target);
        return targets;
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