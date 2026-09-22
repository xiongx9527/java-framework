package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 支持 Redis 的 WebSocket 处理器：通过 Redis 频道实现跨实例广播。
 *
 * @author zzy
 */
@Slf4j
public class RedisWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * Redis消息监听器，用于处理分布式环境下的WebSocket消息
     */
    private MessageListener redisMessageListener;

    /**
     * Redis模板，用于发送消息到Redis
     */
    @Autowired(required = true)
    private StringRedisTemplate stringRedisTemplate;

    public RedisWebSocketHandler(WsConfig wsConfig) {
        super(wsConfig);
        // 初始化Redis消息监听器
        initRedisMessageListener();
    }

    /**
     * 创建分布式广播消息对象
     */
    private DistributedBroadcastMessage createDistributedBroadcastMessage(String excludedServerId, String appId,
                                                                          String clientId, String message) {
        return new DistributedBroadcastMessage(excludedServerId, appId, clientId, message);
    }

    /**
     * 通过Redis向指定客户端广播消息
     *
     * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
     * @param appId            应用ID
     * @param clientId         客户端ID
     * @param message          要发送的消息内容
     */
    @Override
    void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {
        try {
            // 创建分布式广播消息对象
            DistributedBroadcastMessage broadcastMessage = createDistributedBroadcastMessage(
                    excludedServerId, appId, clientId, message);

            // 将消息转换为JSON字符串
            String messageJson = com.alibaba.fastjson2.JSON.toJSONString(broadcastMessage);

            // 从配置中获取广播频道名称
            String broadcastChannel = getBroadcastChannelName();

            // 使用Redis模板发送消息
            stringRedisTemplate.convertAndSend(broadcastChannel, messageJson);

            log.info("Broadcast message sent to Redis channel: {}, excludedServerId: {}, clientId: {}, appId: {}",
                    broadcastChannel, excludedServerId, clientId, appId);
        } catch (Exception e) {
            log.error("Failed to send broadcast message to client via Redis - clientId: {}, appId: {}", clientId, appId, e);
        }
    }

    /**
     * 向所有客户端广播消息
     *
     * @param excludedServerId 需要排除的服务器ID，该服务器的客户端将不会收到广播消息
     * @param appId            应用程序ID，用于标识消息所属的应用
     * @param message          要广播的消息内容
     */
    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
        broadcastMessageToClient(excludedServerId, appId, null, message);
    }

    /**
     * 初始化Redis消息监听器
     */
    private void initRedisMessageListener() {
        this.redisMessageListener = new MyMessageListener(this);
    }

    /**
     * 获取Redis消息监听器
     *
     * @return Redis消息监听器
     */
    public MessageListener getRedisMessageListener() {
        return redisMessageListener;
    }

    @Data
    @AllArgsConstructor
    private static class DistributedBroadcastMessage {
        private String excludedServerId;
        private String appId;
        private String clientId;
        private String message;
    }

    /**
     * Redis消息监听器实现类，用于处理来自其他服务器的WebSocket广播消息
     */
    private static class MyMessageListener implements MessageListener {

        /**
         * 基础WebSocket处理器实例
         */
        private final AbstractWebSocketHandler webSocketHandler;

        private MyMessageListener(AbstractWebSocketHandler webSocketHandler) {
            this.webSocketHandler = webSocketHandler;
        }

        /**
         * 处理从Redis接收到的广播消息
         *
         * @param message 从Redis接收到的消息
         * @param pattern 消息匹配的模式
         */
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                // 解析消息内容
                String messageBody = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                DistributedBroadcastMessage broadcastMessage = com.alibaba.fastjson2.JSON.parseObject(
                        messageBody, DistributedBroadcastMessage.class);

                // 检查消息有效性及是否需要排除当前服务器
                if (broadcastMessage == null) {
                    log.warn("Received null broadcast message from Redis");
                    return;
                }

                String excludedServerId = broadcastMessage.getExcludedServerId();
                String currentServerId = webSocketHandler.getWebSocketServerId();
                if (excludedServerId != null && excludedServerId.equals(currentServerId)) {
                    log.debug("Skipping message for excluded server: {}", excludedServerId);
                    return;
                }

                // 校验必要字段，防止反序列化出空字段导致下游 NPE
                if (broadcastMessage.getAppId() == null || broadcastMessage.getMessage() == null) {
                    log.warn("Received broadcast message with null appId or message, ignoring");
                    return;
                }

                // 根据消息类型决定广播方式，仅限本地投递，不重新发布到Redis避免消息放大循环
                if (StringUtils.isBlank(broadcastMessage.getClientId())) {
                    webSocketHandler.broadcastMessageToAllClientLocalOnly(broadcastMessage.getAppId(), broadcastMessage.getMessage());
                } else {
                    webSocketHandler.broadcastMessageToClientLocalOnly(broadcastMessage.getAppId(), broadcastMessage.getClientId(), broadcastMessage.getMessage());
                }
            } catch (Exception e) {
                log.error("Error processing Redis broadcast message", e);
            }
        }
    }
}
