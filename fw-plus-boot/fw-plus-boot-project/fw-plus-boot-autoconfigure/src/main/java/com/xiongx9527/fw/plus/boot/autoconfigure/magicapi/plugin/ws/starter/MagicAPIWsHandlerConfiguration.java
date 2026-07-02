package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.RedisDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@ConditionalOnProperty(prefix = "magic-api.plugin.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
//@ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
@Configuration
public class MagicAPIWsHandlerConfiguration implements BaseService {

    /**
     * 创建基于Redis的WebSocket处理器Bean
     *
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param wsSubscribeInfoMagicResourceStorage WebSocket订阅信息存储
     * @return RedisDynamicWebSocketHandler实例
     */
    @Bean
    //@ConditionalOnProperty(prefix = "spring.redis", name = "host", havingValue = "true", matchIfMissing = true)
    @DependsOn(value = {"wsDataSourceInfoMagicResourceStorage", "wsSubscribeInfoMagicResourceStorage"})
    public BaseDynamicWebSocketHandler redisDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage
            , WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage) {
        return new RedisDynamicWebSocketHandler(magicDataSourceStorage, wsSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建Redis消息监听容器Bean，用于处理WebSocket消息的分布式传输
     *
     * @return RedisMessageListenerContainer实例
     */
    @Bean
    //@ConditionalOnClass(name = {"org.springframework.data.redis.connection.RedisConnectionFactory"})
    public MagicAPIRedisMessageListenerContainer redisMessageListenerContainer() {
        MagicAPIRedisMessageListenerContainer container = new MagicAPIRedisMessageListenerContainer();
        return container;
    }

    public static class MagicAPIRedisMessageListenerContainer extends RedisMessageListenerContainer implements ApplicationContextAware, BaseService {

        /**
         * 设置应用程序上下文，并初始化Redis连接和消息监听器
         *
         * @param applicationContext 应用程序上下文对象
         * @throws BeansException 当Bean获取失败时抛出异常
         */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            // 获取Redis连接工厂并设置连接
            RedisConnectionFactory redisConnectionFactory = SpringUtils.getBean(RedisConnectionFactory.class);
            this.setConnectionFactory(redisConnectionFactory);

            // 添加Redis消息监听器用于WebSocket消息处理
            RedisDynamicWebSocketHandler webSocketHandler = SpringUtils.getBean(RedisDynamicWebSocketHandler.class);
            this.addMessageListener(webSocketHandler.getRedisMessageListener(),
                    new ChannelTopic(getBroadcastChannelName()));
            //this.addMessageListener(webSocketHandler.getRedisBroadcastMessageListener(),
            //        new ChannelTopic(getBroadcastChannelName()));
        }


    }


}
