package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster;

import com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry;

/**
 * Redis 集群注册表契约，在 {@link BaseServerRegistry} 之上补充消费端需要的本地投递能力。
 * <p>
 * 集群消息经 Redis 到达各节点后，只能做纯本地投递：若再次发布回频道，消息会在节点间
 * 无限循环。因此本契约重写 {@link #sendOther(String, String, String)} 与
 * {@link #broadcastOther(String, String)}：定向消息仅投递给持有该会话的节点，广播消息
 * 只做本地分发；{@link #hasSession(String)} 供定向投递判断本机是否持有会话。
 * </p>
 */
public interface RedisClusterRegistry extends BaseServerRegistry {

    /**
     * 本机是否持有指定会话。
     *
     * @param sessionId 会话 id
     * @return 持有返回 {@code true}
     */
    boolean hasSession(String sessionId);
}
