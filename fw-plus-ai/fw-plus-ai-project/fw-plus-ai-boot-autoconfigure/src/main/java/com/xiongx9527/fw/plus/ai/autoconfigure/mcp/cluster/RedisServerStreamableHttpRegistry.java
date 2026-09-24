package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerStreamableHttpRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * 带 Redis 集群广播的 Streamable HTTP 注册表。
 * <p>
 * 本机协议入口（{@link #sendMsg}、{@link #broadcastMsg}）：本机有 GET 事件流直接投递，
 * 没有则经 Redis 转交；广播先下发本机会话，再经 Redis 让其它节点各自分发。集群入口
 * （{@link #sendOther}、{@link #broadcastOther}）只做纯本地投递，不再发布回频道，否则
 * 消息会在节点间无限循环。
 * </p>
 */
@Slf4j
public class RedisServerStreamableHttpRegistry extends DefaultServerStreamableHttpRegistry
        implements RedisClusterRegistry {

    private final McpRedisBroadcaster broadcaster;

    public RedisServerStreamableHttpRegistry(McpServer server, McpRedisBroadcaster broadcaster) {
        super(server, broadcaster.getServerId());
        this.broadcaster = broadcaster;
    }

    /**
     * 定向发送：本机有该会话的事件流则本地投递，否则经 Redis 转交给持有该流的节点。
     *
     * @param sessionId 会话 id
     * @param json      JSON 消息体
     */
    @Override
    public void sendMsg(String sessionId, String json) {
        if (hasSession(sessionId)) {
            super.sendMsg(sessionId, json);
            return;
        }

        broadcastToCluster(sessionId, json);
    }

    @Override
    public void broadcastMsg(String json) {
        super.broadcastMsg(json);
        // sessionId 为空表示广播给集群内所有会话，由各节点自行分发。
        broadcastToCluster(null, json);
    }

    @Override
    public boolean hasSession(String sessionId) {
        return resolveStreams().containsKey(sessionId);
    }

    /**
     * 集群定向投递：本机持有该会话的 GET 事件流才投递，否则静默跳过——持有该流的节点会
     * 各自投递，没有该流的节点若再经 Redis 转发会互相打转。
     *
     * @param serverId  消息来源节点标识
     * @param sessionId 目标会话 id
     * @param json      JSON 消息体
     */
    @Override
    public void sendOther(String serverId, String sessionId, String json) {
        if (!hasSession(sessionId))
            return;

        // 父类内含来源守卫，来源为本机时不会重复投递。
        super.sendOther(serverId, sessionId, json);
    }

    /**
     * 集群广播投递：仅分发本机会话，不再发布回 Redis 频道。
     *
     * @param serverId 消息来源节点标识
     * @param json     JSON 消息体
     */
    @Override
    public void broadcastOther(String serverId, String json) {
        if (!broadcaster.getServerId().equals(serverId))
            super.broadcastMsg(json);
    }

    private void broadcastToCluster(String sessionId, String json) {
        if (broadcaster == null)
            return;

        try {
            broadcaster.broadcast(sessionId, json);
        } catch (Exception e) {
            log.warn("Failed to broadcast MCP message via redis: {}", e.getMessage());
        }
    }
}
