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
     * 父类 ServerStreamableHttp#streams 字段，仅在类加载时解析一次。
     */
    private static final Field STREAMS_FIELD;

    static {
        try {
            Field field = ServerStreamableHttp.class.getDeclaredField("streams");
            field.setAccessible(true);
            STREAMS_FIELD = field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ServerStreamableHttp#streams not found", e);
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
     * 发送消息
     *
     * @param sessionId 会话 ID
     * @param json      消息内容
     */
    public void send(String sessionId, String json) {
        this.sendMsg(sessionId, json);
    }

    /**
     * 发送消息
     *
     * @param sessionId 会话 ID
     * @param json      消息内容
     */
    public abstract void sendMsg(String sessionId, String json) ;

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
     * 广播消息
     *
     * @param json 消息内容
     */
    public abstract void broadcastMsg(String json) ;

    /**
     * Sends a message to a specific client on another server.
     *
     * @param serverId The ID of the target server.
     * @param sessionId The session ID of the target client.
     * @param json The JSON data to send.
     */
    @Override
    public final void sendOther(String serverId, String sessionId, String json) {
        if (!this.serverId.equals(serverId)) {
            this.sendMsg(sessionId, json);
        }
    }

    /**
     * Broadcasts a message to all clients on another server.
     *
     * @param serverId The ID of the target server.
     * @param data The data to broadcast.
     */
    @Override
    public final void broadcastOther(String serverId, String data) {
        if (!this.serverId.equals(serverId)) {
            this.broadcastMsg(data);
        }
    }

}
