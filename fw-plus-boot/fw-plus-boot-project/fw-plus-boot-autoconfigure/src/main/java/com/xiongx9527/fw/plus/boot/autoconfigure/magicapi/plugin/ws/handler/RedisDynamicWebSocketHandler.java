package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 支持Redis的动态WebSocket处理器
 */
@Slf4j
public class RedisDynamicWebSocketHandler extends BaseDynamicWebSocketHandler {
    
    /**
     * Redis消息监听器，用于处理分布式环境下的WebSocket消息
     */
    private MessageListener redisMessageListener;
    
    /**
     * Redis模板，用于发送消息到Redis
     */
    @Autowired(required = true)
    private StringRedisTemplate stringRedisTemplate;
    

    /**
     * Redis WebSocket处理器构造函数
     * 
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param magicSubscribeStorage WebSocket订阅信息存储
     */
    public RedisDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage,
                                        WsSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        super(magicDataSourceStorage,magicSubscribeStorage);
        // 初始化Redis消息监听器
        initRedisMessageListener();
    }


    /**
     * 创建分布式广播消息对象
     *
     * @param excludedServerId 需要排除的服务器ID
     * @param appId 应用ID
     * @param clientId 客户端ID
     * @param message 消息内容
     * @return 分布式广播消息对象
     */
    private DistributedBroadcastMessage createDistributedBroadcastMessage(String excludedServerId, String appId,String clientId,  String message) {
        return new DistributedBroadcastMessage(excludedServerId, appId, clientId, message);
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
     * @param appId 应用程序ID，用于标识消息所属的应用
     * @param message 要广播的消息内容
     */
    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
        // 调用通用的广播方法，传入null表示向所有客户端广播
        broadcastMessageToClient(excludedServerId, null, appId, message);
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
        private String message;
    }

    /**
     * Redis消息监听器实现类，用于处理来自其他服务器的WebSocket广播消息
     */
    private static class MyMessageListener implements MessageListener {

        /**
         * 基础WebSocket处理器实例
         */
        private final BaseDynamicWebSocketHandler baseDynamicWebSocketHandler;

        /**
         * 构造函数
         * 
         * @param baseDynamicWebSocketHandler 基础WebSocket处理器实例
         */
        private MyMessageListener(BaseDynamicWebSocketHandler baseDynamicWebSocketHandler) {
            this.baseDynamicWebSocketHandler = baseDynamicWebSocketHandler;
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
                String messageBody = new String(message.getBody());
                DistributedBroadcastMessage broadcastMessage = com.alibaba.fastjson2.JSON.parseObject(
                    messageBody, DistributedBroadcastMessage.class);
                
                // 检查消息有效性及是否需要排除当前服务器
                if (broadcastMessage == null) {
                    log.warn("Received null broadcast message from Redis");
                    return;
                }
                
                String excludedServerId = broadcastMessage.getExcludedServerId();
                String currentServerId = baseDynamicWebSocketHandler.getWebSocketServerId();
                if (excludedServerId != null && excludedServerId.equals(currentServerId)) {
                    log.debug("Skipping message for excluded server: {}", excludedServerId);
                    return;
                }
                
                // 根据消息类型决定广播方式
                if (StringUtils.isBlank(broadcastMessage.getClientId())) {
                    // 广播给所有客户端
                    baseDynamicWebSocketHandler.broadcastMessageToAllClient(broadcastMessage.getAppId(), broadcastMessage.getMessage());
                } else {
                    // 广播给指定客户端
                    baseDynamicWebSocketHandler.broadcastMessageToClient(broadcastMessage.getClientId(), broadcastMessage.getAppId(), broadcastMessage.getMessage());
                }
            } catch (Exception e) {
                log.error("Error processing Redis broadcast message", e);
            }
        }
    }

}
