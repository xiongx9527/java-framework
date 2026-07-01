package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.BaseDynamicSseHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.RedisDynamicSseHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@ConditionalOnProperty(prefix = "magic-api.plugin.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
@Configuration
public class MagicAPISseHandlerConfiguration implements BaseService {

    /**
     * 创建Redis动态SSE处理器Bean
     * 该处理器依赖于SSE数据源信息存储和订阅信息存储两个Bean
     *
     * @param magicDataSourceStorage SSE数据源信息存储器，用于管理数据源配置
     * @param ssSubscribeInfoMagicResourceStorage SSE订阅信息存储器，用于管理订阅关系
     * @return BaseDynamicSseHandler Redis动态SSE处理器实例
     */
    @Bean
    @DependsOn(value = {"sseDataSourceInfoMagicResourceStorage", "sseSubscribeInfoMagicResourceStorage"})
    public BaseDynamicSseHandler redisDynamicSseHandler(SseDataSourceInfoMagicResourceStorage magicDataSourceStorage
            , SseSubscribeInfoMagicResourceStorage ssSubscribeInfoMagicResourceStorage) {
        return new RedisDynamicSseHandler(magicDataSourceStorage, ssSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建Redis消息监听容器Bean
     * 用于监听和处理Redis中的消息
     *
     * @return MagicAPIRedisMessageListenerContainer Redis消息监听容器实例
     */
    @Bean
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

            // 添加Redis消息监听器用于SSE消息处理
            RedisDynamicSseHandler sseHandler = SpringUtils.getBean(RedisDynamicSseHandler.class);
            this.addMessageListener(sseHandler.getRedisMessageListener(),
                    new ChannelTopic(getBroadcastChannelName()));
        }
    }


}
