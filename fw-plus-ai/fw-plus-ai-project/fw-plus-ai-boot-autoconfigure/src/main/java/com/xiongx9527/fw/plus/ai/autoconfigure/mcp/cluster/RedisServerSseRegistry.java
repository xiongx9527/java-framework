package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerSseRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * 带 Redis 集群广播的 SSE 注册表。
 * <p>
 * 本机协议入口（{@link #sendMsg}、{@link #broadcastMsg}）：本机有会话直接投递，没有则经
 * Redis 转交；广播先下发本机会话，再经 Redis 让其它节点各自分发。集群入口
 * （{@link #sendOther}、{@link #broadcastOther}）只做纯本地投递，不再发布回频道，否则
 * 消息会在节点间无限循环。
 * </p>
 */
@Slf4j
public class RedisServerSseRegistry extends DefaultServerSseRegistry implements RedisClusterRegistry {

    private final McpRedisBroadcaster broadcaster;

    public RedisServerSseRegistry(McpServer server, McpRedisBroadcaster broadcaster) {
        super(server, broadcaster.getServerId());
        this.broadcaster = broadcaster;
    }

    /**
     * 定向发送：本机有该会话则本地投递，否则经 Redis 转交给持有该会话的节点。
     *
     * @param uuid 会话 id
     * @param data JSON 消息体
     */
    @Override
    public void sendMsg(String uuid, String data) {
        if (hasSession(uuid)) {
            super.sendMsg(uuid, data);
            return;
        }

        broadcastToCluster(uuid, data);
    }

    @Override
    public void broadcastMsg(String data) {
        super.broadcastMsg(data);
        // sessionId 为空表示广播给集群内所有会话，由各节点自行分发。
        broadcastToCluster(null, data);
    }

    @Override
    public boolean hasSession(String sessionId) {
        return resolveConnections().containsKey(sessionId);
    }

    /**
     * 集群定向投递：本机持有该会话才投递，否则静默跳过——持有该会话的节点会各自投递，
     * 没有该会话的节点若再经 Redis 转发会互相打转。
     *
     * @param serverId  消息来源节点标识
     * @param sessionId 目标会话 id
     * @param data      JSON 消息体
     */
    @Override
    public void sendOther(String serverId, String sessionId, String data) {
        if (!hasSession(sessionId))
            return;

        // 父类内含来源守卫，来源为本机时不会重复投递。
        super.sendOther(serverId, sessionId, data);
    }

    /**
     * 集群广播投递：仅分发本机会话，不再发布回 Redis 频道。
     *
     * @param serverId 消息来源节点标识
     * @param data     JSON 消息体
     */
    @Override
    public void broadcastOther(String serverId, String data) {
        if (!broadcaster.getServerId().equals(serverId))
            super.broadcastMsg(data);
    }

    private void broadcastToCluster(String sessionId, String data) {
        if (broadcaster == null)
            return;

        try {
            broadcaster.broadcast(sessionId, data);
        } catch (Exception e) {
            log.warn("Failed to broadcast MCP message via redis: {}", e.getMessage());
        }
    }
}
