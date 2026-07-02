package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.utils.JsonUtils;


@Slf4j
public class MagicAPIRedisMessageListenerContainer extends RedisMessageListenerContainer implements ApplicationContextAware {

    /**
     * 集群配置信息
     *
     * 该字段存储了当前集群的配置参数，用于指导集群的行为和设置
     */
    private final ClusterConfig config;

    /**
     * 构造方法，初始化MagicAPIRedisMessageListenerContainer实例
     * @param config 集群配置信息，用于配置消息监听器的频道等参数
     */
    public MagicAPIRedisMessageListenerContainer(ClusterConfig config) {
        this.config = config;
    }

    /**
     * 设置应用程序上下文，并初始化Redis消息监听器
     * @param applicationContext Spring应用程序上下文
     * @throws BeansException 当获取Bean失败时抛出此异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("开启集群通知监听， Redis channel: {}", config.getChannel());
        // 获取Redis连接工厂并设置连接
        RedisConnectionFactory redisConnectionFactory = SpringUtils.getBean(RedisConnectionFactory.class);
        this.setConnectionFactory(redisConnectionFactory);

        MagicAPIService magicAPIService = SpringUtils.getBean(MagicAPIService.class);
        // 添加消息监听器，监听指定频道的消息并处理通知
        this.addMessageListener((message, pattern) -> magicAPIService.processNotify(JsonUtils.readValue(message.getBody(), MagicNotify.class)), ChannelTopic.of(config.getChannel()));
    }

}
