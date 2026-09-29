package com.xiongx9527.fw.plus.ai.mcp.spi;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.StreamSession;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 服务端长连接注册类
 */
public abstract class ServerStreamableHttpRegistry extends ServerStreamableHttp  implements BaseServerRegistry {

    /**
     * 父类 ServerStreamableHttp#streams 字段（GET 事件流），仅在类加载时解析一次。
     */
    private static final Field STREAMS_FIELD;

    /**
     * 父类 ServerStreamableHttp#sessions 字段（全部已初始化 HTTP 会话），仅在类加载时解析一次。
     */
    private static final Field SESSIONS_FIELD;

    static {
        try {
            Field field = ServerStreamableHttp.class.getDeclaredField("streams");
            field.setAccessible(true);
            STREAMS_FIELD = field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ServerStreamableHttp#streams not found", e);
        }

        try {
            Field field = ServerStreamableHttp.class.getDeclaredField("sessions");
            field.setAccessible(true);
            SESSIONS_FIELD = field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ServerStreamableHttp#sessions not found", e);
        }
    }

    /**
     * 服务端 ID
     */
    private final String serverId;

    /**
     * 构造函数
     *
     * @param serverId 服务端 ID
     * @param server   服务端实例
     */
    public ServerStreamableHttpRegistry(McpServer server,String serverId) {
        super(server);
        this.serverId = serverId;
    }

    /**
     * 反射获取父类实例上的 streams 对象。
     *
     * @return 父类 streams 字段引用的实例，本身即 ConcurrentHashMap，无需再次包装
     */
    @SuppressWarnings("unchecked")
    protected Map<String, StreamSession> resolveStreams() {
        try {
            return MapUtils.emptyIfNull((Map<String, StreamSession>) STREAMS_FIELD.get(this));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access ServerStreamableHttp#streams", e);
        }
    }

    /**
     * 判断本注册表是否持有指定会话，用于多实例下的会话归属校验。
     * <p>
     * initialize 建立的会话存在于父类 {@code sessions} map；只有客户端额外建立 GET
     * 事件流（服务器主动推送）时才会出现在 {@code streams} map。归属校验应覆盖两者。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 本实例是否持有该会话
     */
    public boolean hasSession(String sessionId) {
        return resolveStreams().containsKey(sessionId) || resolveSessions().containsKey(sessionId);
    }

    /**
     * 反射获取父类实例上的 sessions 对象。
     *
     * @return 父类 sessions 字段引用的实例，本身即 ConcurrentHashMap
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveSessions() {
        try {
            return MapUtils.emptyIfNull((Map<String, Object>) SESSIONS_FIELD.get(this));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access ServerStreamableHttp#sessions", e);
        }
    }

    /**
     * 发送消息
     *
     * @param sessionId 会话 ID
     * @param json      消息内容
     */
    public void send(String sessionId, String json) {
        this.sendMsg(sessionId, json);
    }

    /**
     * 广播消息
     *
     *
     * @param json 消息内容
     */
    public void broadcast(String json) {
        this.broadcastMsg(json);
    }

    /**
     * Sends a message to a specific client on another server.
     * <p>
     * 非 final：集群子类需要按本地 GET 事件流持有情况决定投递还是静默跳过，避免消息经
     * 中间节点反复转发。重写时应保留 {@code serverId} 来源守卫语义。
     * </p>
     *
     * @param serverId The ID of the target server.
     * @param sessionId The session ID of the target client.
     * @param json The JSON data to send.
     */
    @Override
    public void sendOther(String serverId, String sessionId, String json) {
        if (!this.serverId.equals(serverId)) {
            this.sendMsg(sessionId, json);
        }
    }

    /**
     * Broadcasts a message to all clients on another server.
     * <p>
     * 非 final：集群子类重写为仅本地分发，不再把来自其它节点的消息发布回集群频道。
     * </p>
     *
     * @param serverId The ID of the target server.
     * @param data The data to broadcast.
     */
    @Override
    public void broadcastOther(String serverId, String data) {
        if (!this.serverId.equals(serverId)) {
            this.broadcastMsg(data);
        }
    }

}
