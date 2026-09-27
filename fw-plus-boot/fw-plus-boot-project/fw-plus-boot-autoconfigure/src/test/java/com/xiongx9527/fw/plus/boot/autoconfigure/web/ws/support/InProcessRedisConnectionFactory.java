package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.data.redis.connection.util.ByteArrayWrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内假 Redis 连接工厂：让「跨实例广播」在没有外部 Redis 的机器上也能跑通。
 *
 * <p>发布方的 {@code convertAndSend} 经 {@code publish} 进入本工厂，报文被直接回送给已订阅该频道的
 * 监听器（即 {@code RedisMessageListenerContainer} 注册的 {@code SynchronizingMessageListener}）。此后由
 * <b>真实的</b> {@code RedisMessageListenerContainer}、其 {@code redis-ws-} 任务执行器与
 * {@code WsBroadcastMessageListener} 完成反序列化、serverId 排除与本地投递。</p>
 *
 * <p>因此被替身替换的只有连接与网络层：监听容器、监听器、广播消息体、频道名解析与本地投递全部保持
 * 被测对象。订阅注册完成时会通知监听器，容器因此不会停在「订阅注册超时」上。</p>
 *
 * <p>订阅登记在进程内 broker（本工厂的频道表）上，而不是绑定在单个连接上：{@code close()} 只标记连接关闭，
 * 不撤销已登记的订阅。真实部署的广播监听容器跑在 Lettuce（非阻塞连接）上，注册完频道订阅并不关闭连接；
 * 而 Spring Data Redis 对非 Lettuce 工厂会改用 {@code BlockingSubscriber}，它假设 {@code subscribe()}
 * 会阻塞到连接关闭，方法一返回就关闭连接。若让 {@code close()} 顺手退订，订阅会在注册后立刻消失，
 * 因此连接生命周期与订阅生命周期必须分开对待，取消订阅只走 {@code unsubscribe} / {@code Subscription#close()}。</p>
 *
 * <p>只存在于 {@code src/test}，不新增 Maven 依赖。</p>
 *
 * @author zzy
 */
public class InProcessRedisConnectionFactory implements RedisConnectionFactory {

    private final Map<ByteArrayWrapper, List<MessageListener>> channelSubscribers = new ConcurrentHashMap<>();

    @Override
    public RedisConnection getConnection() {
        return (RedisConnection) Proxy.newProxyInstance(
                RedisConnection.class.getClassLoader(),
                new Class<?>[] {RedisConnection.class},
                new InProcessConnection());
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return null;
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return false;
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return null;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return null;
    }

    /**
     * 指定频道当前的订阅者数量，供测试断言监听容器确实完成了订阅。
     *
     * @param channel 频道名
     * @return 订阅者数量
     */
    public int subscriberCount(String channel) {
        List<MessageListener> listeners = channelSubscribers.get(new ByteArrayWrapper(channelBytes(channel)));
        return listeners == null ? 0 : listeners.size();
    }

    /**
     * 是否已有监听器订阅指定频道。
     *
     * @param channel 频道名
     * @return 已订阅返回 true
     */
    public boolean isSubscribed(String channel) {
        return subscriberCount(channel) > 0;
    }

    /**
     * 把广播报文回送给该频道的所有订阅者，回报收到报文的订阅者数量。
     *
     * @param channel 广播频道（UTF-8 字节）
     * @param message 报文内容（UTF-8 字节）
     * @return 收到报文的订阅者数量
     */
    long publish(byte[] channel, byte[] message) {
        List<MessageListener> listeners = channelSubscribers.get(new ByteArrayWrapper(channel));
        if (listeners == null || listeners.isEmpty()) {
            return 0L;
        }
        // 拷贝一份再遍历：监听器可能在回调里退订
        List<MessageListener> recipients = new ArrayList<>(listeners);
        Message redisMessage = new DefaultMessage(channel, message);
        for (MessageListener listener : recipients) {
            listener.onMessage(redisMessage, null);
        }
        return recipients.size();
    }

    private static byte[] channelBytes(String channel) {
        return channel.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 单个连接的处理器。{@code RedisConnection} 方法众多，用 JDK 代理按方法名分发，
     * 只实现广播链路真正用到的方法，其余返回安全的默认值。
     */
    private final class InProcessConnection implements InvocationHandler, Subscription {

        private final Set<ByteArrayWrapper> subscribedChannels = new HashSet<>();

        private MessageListener listener;

        private volatile boolean closed;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (hasListenerArgument(args) && ("subscribe".equals(name) || "pSubscribe".equals(name))) {
                return doSubscribe((MessageListener) args[0], toChannels(args[1]));
            }
            if (("unsubscribe".equals(name) || "pUnsubscribe".equals(name))) {
                unsubscribeAll();
                return expectsSubscription(method) ? this : null;
            }
            if ("publish".equals(name) && isPublish(args)) {
                return publish((byte[]) args[0], (byte[]) args[1]);
            }
            if ("isSubscribed".equals(name)) {
                return !subscribedChannels.isEmpty();
            }
            if ("getSubscription".equals(name)) {
                return this;
            }
            if ("isClosed".equals(name)) {
                return closed;
            }
            if ("close".equals(name)) {
                // 只关闭连接本身：订阅归进程内 broker 所有，容器注册后即关闭连接属正常时序
                closed = true;
                return null;
            }
            if ("getNativeConnection".equals(name)) {
                return proxy;
            }
            if ("ping".equals(name)) {
                return "PONG";
            }
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            return defaultValueFor(method);
        }

        /**
         * 注册订阅并通知监听器。通知是必须的：容器会阻塞等待订阅注册完成的回调。
         */
        private Object doSubscribe(MessageListener listener, List<byte[]> channels) {
            this.listener = listener;
            this.closed = false;
            Set<ByteArrayWrapper> registered = new HashSet<>();
            for (byte[] channel : channels) {
                ByteArrayWrapper wrapper = new ByteArrayWrapper(channel);
                List<MessageListener> listeners = channelSubscribers.computeIfAbsent(
                        wrapper, key -> new CopyOnWriteArrayList<>());
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
                subscribedChannels.add(wrapper);
                registered.add(wrapper);
            }
            // 先置好订阅状态，再回调：监听器收到回调时订阅已经可用
            for (byte[] channel : channels) {
                notifySubscribed(listener, channel, 1L);
            }
            return this;
        }

        private void unsubscribeAll() {
            List<byte[]> channels = new ArrayList<>();
            for (ByteArrayWrapper wrapper : subscribedChannels) {
                channels.add(wrapper.getArray());
            }
            for (byte[] channel : channels) {
                List<MessageListener> listeners = channelSubscribers.get(new ByteArrayWrapper(channel));
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        channelSubscribers.remove(new ByteArrayWrapper(channel), listeners);
                    }
                }
            }
            subscribedChannels.clear();
            MessageListener current = listener;
            listener = null;
            if (current != null) {
                for (byte[] channel : channels) {
                    notifySubscribed(current, channel, 0L, false);
                }
            }
        }

        private void notifySubscribed(MessageListener listener, byte[] channel, long count, boolean subscribed) {
            if (listener instanceof SubscriptionListener) {
                if (subscribed) {
                    ((SubscriptionListener) listener).onChannelSubscribed(channel, count);
                } else {
                    ((SubscriptionListener) listener).onChannelUnsubscribed(channel, count);
                }
            }
        }

        private void notifySubscribed(MessageListener listener, byte[] channel, long count) {
            notifySubscribed(listener, channel, count, true);
        }

        @Override
        public Collection<byte[]> getChannels() {
            List<byte[]> channels = new ArrayList<>();
            for (ByteArrayWrapper wrapper : subscribedChannels) {
                channels.add(wrapper.getArray().clone());
            }
            return channels;
        }

        @Override
        public Collection<byte[]> getPatterns() {
            return Collections.emptyList();
        }

        @Override
        public MessageListener getListener() {
            return listener;
        }

        @Override
        public boolean isAlive() {
            return !closed;
        }

        @Override
        public void subscribe(byte[]... channels) {
            MessageListener current = listener;
            if (current != null && channels != null) {
                doSubscribe(current, toChannels(channels));
            }
        }

        @Override
        public void pSubscribe(byte[]... patterns) {
            subscribe(patterns);
        }

        @Override
        public void unsubscribe() {
            unsubscribeAll();
        }

        @Override
        public void unsubscribe(byte[]... channels) {
            unsubscribeAll();
        }

        @Override
        public void pUnsubscribe() {
            unsubscribeAll();
        }

        @Override
        public void pUnsubscribe(byte[]... patterns) {
            unsubscribeAll();
        }

        @Override
        public void close() {
            unsubscribeAll();
            closed = true;
        }
    }

    private static boolean hasListenerArgument(Object[] args) {
        return args != null && args.length >= 2 && args[0] instanceof MessageListener;
    }

    private static boolean isPublish(Object[] args) {
        return args != null && args.length == 2 && args[0] instanceof byte[] && args[1] instanceof byte[];
    }

    private static List<byte[]> toChannels(Object raw) {
        List<byte[]> channels = new ArrayList<>();
        if (raw instanceof byte[][]) {
            for (byte[] channel : (byte[][]) raw) {
                channels.add(channel);
            }
        } else if (raw instanceof byte[]) {
            channels.add((byte[]) raw);
        }
        return channels;
    }

    private static boolean expectsSubscription(Method method) {
        return Subscription.class.isAssignableFrom(method.getReturnType());
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "InProcessRedisConnection";
            default:
                return method.getReturnType() == void.class ? null : null;
        }
    }

    private static Object defaultValueFor(Method method) {
        Class<?> type = method.getReturnType();
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        return null;
    }
}
