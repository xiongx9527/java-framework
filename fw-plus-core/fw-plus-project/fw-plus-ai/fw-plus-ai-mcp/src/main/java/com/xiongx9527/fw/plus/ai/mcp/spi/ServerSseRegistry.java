package com.xiongx9527.fw.plus.ai.mcp.spi;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.ServerSse;
import com.ajaxjs.mcp.server.model.SseSession;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;


/**
 * 继承 {@link ServerSse}，通过反射访问父类的 package-private {@code connections} 字段。
 */
public abstract class ServerSseRegistry extends ServerSse implements BaseServerRegistry {

    /**
     * 父类 ServerSse#connections 字段引用，仅在类加载时解析一次。
     */
    private static final Field CONNECTIONS_FIELD;

    /**
     * 父类 ServerSse#removeConnection(String, SseSession) 私有方法引用，仅在类加载时解析一次。
     */
    private static final Method REMOVE_CONNECTION_METHOD;

    static {
        try {
            Field field = ServerSse.class.getDeclaredField("connections");
            field.setAccessible(true);
            CONNECTIONS_FIELD = field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("ServerSse#connections not found", e);
        }

        try {
            Method method = ServerSse.class.getDeclaredMethod("removeConnection", String.class, SseSession.class);
            method.setAccessible(true);
            REMOVE_CONNECTION_METHOD = method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("ServerSse#removeConnection(String, SseSession) not found", e);
        }
    }

    /**
     * 服务端 ID
     */
    private final String serverId;

    /**
     * 构造函数
     *
     * @param server   服务端
     * @param serverId 服务端 ID
     */
    public ServerSseRegistry(McpServer server, String serverId) {
        super(server);
        this.serverId = serverId;
    }

    /**
     * 反射获取父类实例上的 {@code connections} 对象。
     *
     * @return 父类 {@code connections} 字段引用的实例，本身即 {@link java.util.concurrent.ConcurrentHashMap}
     */
    @SuppressWarnings("unchecked")
    protected Map<String, SseSession> resolveConnections() {
        try {
            return (Map<String, SseSession>) CONNECTIONS_FIELD.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access ServerSse#connections", e);
        }
    }

    /**
     * 反射调用父类私有方法 {@code removeConnection(String, SseSession)}：
     * <pre>
     * private void removeConnection(String clientId, SseSession session) {
     *     if (connections.remove(clientId, session)) {
     *         session.close();
     *         server.removeSession(clientId);
     *     }
     * }
     * </pre>
     *
     * <p>与公开的单参重载 {@link ServerSse#removeConnection(String)} 的区别：先做
     * {@code Map.remove(key, value)} 条件匹配，仅当当前映射的 session 与传入 session
     * 为同一实例时才移除并清理，避免误删已被新连接替换的会话。</p>
     *
     * @param clientId 逻辑客户端/会话 id
     * @param session  期望被移除的 SSE 会话实例
     */
    protected void removeConnection(String clientId, SseSession session) {
        try {
            REMOVE_CONNECTION_METHOD.invoke(this, clientId, session);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access ServerSse#removeConnection(String, SseSession)", e);
        } catch (InvocationTargetException e) {
            // 还原父类方法内部抛出的原始异常
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if (cause instanceof Error)
                throw (Error) cause;
            throw new IllegalStateException("ServerSse#removeConnection(String, SseSession) failed", cause);
        }
    }

    /**
     * 返回消息
     *
     * @param uuid 会话 ID
     * @param data 消息内容
     */
    public final void send(String uuid, String data) {
        this.sendMsg(uuid, data);
    }

    /**
     * 广播消息
     *
     * @param data 消息内容
     */
    public final void broadcast(String data) {
        this.broadcastMsg(data);
    }


    /**
     * Sends a message to a specific client on another server.
     * <p>
     * 非 final：集群子类需要按本机会话持有情况决定投递还是静默跳过，避免消息经中间节点
     * 反复转发。重写时应保留 {@code serverId} 来源守卫语义。
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
