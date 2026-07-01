package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class RedisDynamicSseHandler extends BaseDynamicSseHandler {

    /**
     * Redis消息监听器实例
     * 用于监听Redis中的消息队列，处理接收到的消息
     */
    private MessageListener redisMessageListener;

    /**
     * Redis字符串模板操作类
     * 提供对Redis中字符串类型数据的操作方法，包括set、get、delete等操作
     * 该属性为必需注入，确保Redis操作功能正常运行
     */
    @Autowired(required = true)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis动态SSE处理器构造函数
     * @param magicDataSourceStorage 数据源信息存储器
     * @param magicSubscribeStorage 订阅信息存储器
     */
    public RedisDynamicSseHandler(SseDataSourceInfoMagicResourceStorage magicDataSourceStorage,
                                  SseSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        super(magicDataSourceStorage,magicSubscribeStorage);
        // 初始化Redis消息监听器
        initRedisMessageListener();
    }

    /**
     * 创建分布式广播消息对象
     * @param excludedServerId 需要排除的服务器ID
     * @param appId 应用ID
     * @param clientId 客户端ID
     * @param message 消息内容
     * @return 分布式广播消息对象
     */
    private DistributedBroadcastMessage createDistributedBroadcastMessage(String excludedServerId, String appId, String clientId, String eventName, String message) {
        return new DistributedBroadcastMessage(excludedServerId, appId, clientId, eventName, message);
    }

    /**
     * 通过Redis向指定客户端广播消息
     * 
     * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
     * @param appId 应用ID
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    @Override
    void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String eventName, String message) {
        try {
            DistributedBroadcastMessage broadcastMessage = createDistributedBroadcastMessage(
                    excludedServerId, appId, clientId, eventName, message);
            String messageJson = com.alibaba.fastjson2.JSON.toJSONString(broadcastMessage);
            String broadcastChannel = getBroadcastChannelName();
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
     * @param appId 应用程序ID，用于标识消息所属的应用
     * @param message 要广播的消息内容
     */
    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String eventName, String message) {
        broadcastMessageToClient(excludedServerId, appId, null, eventName, message);
    }
    
    /**
     * 初始化Redis消息监听器
     */
    private void initRedisMessageListener() {
        // 初始化点对点消息监听器
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
        private String eventName;
        private String message;
    }

    private static class MyMessageListener implements MessageListener {

        private final BaseDynamicSseHandler baseDynamicSseHandler;

        private MyMessageListener(BaseDynamicSseHandler baseDynamicSseHandler) {
            this.baseDynamicSseHandler = baseDynamicSseHandler;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                // 解析消息内容
                String messageBody = new String(message.getBody());
                DistributedBroadcastMessage broadcastMessage = com.alibaba.fastjson2.JSON.parseObject(
                    messageBody, DistributedBroadcastMessage.class);
                
                // 检查消息有效性及是否需要排除当前服务器
                if (broadcastMessage == null) {
                    log.warn("Received null broadcast message from Redis");
                    return;
                }
                
                String excludedServerId = broadcastMessage.getExcludedServerId();
                String currentServerId = baseDynamicSseHandler.getSseServerId();
                if (excludedServerId != null && excludedServerId.equals(currentServerId)) {
                    log.debug("Skipping message for excluded server: {}", excludedServerId);
                    return;
                }
                
                // 仅本地投递，避免再次触发 Redis 广播导致无限循环
                if (broadcastMessage.getClientId() == null) {
                    baseDynamicSseHandler.deliverToLocalClients(broadcastMessage.getAppId(),
                            broadcastMessage.getEventName(), broadcastMessage.getMessage());
                } else {
                    baseDynamicSseHandler.deliverToLocalClient(broadcastMessage.getAppId(),
                            broadcastMessage.getClientId(), broadcastMessage.getEventName(), broadcastMessage.getMessage());
                }
            } catch (Exception e) {
                log.error("Error processing Redis broadcast message", e);
            }
        }
    }

}
