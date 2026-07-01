package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.utils.JsonUtils;

import javax.annotation.PostConstruct;

@Slf4j
public class MagicAPIRedisMessageListenerContainer extends RedisMessageListenerContainer {

    /**
     * 集群配置信息
     */
    private final ClusterConfig config;
    
    /**
     * Redis连接工厂
     */
    private final RedisConnectionFactory connectionFactory;
    
    /**
     * MagicAPI服务实例
     */
    private final MagicAPIService magicAPIService;

    /**
     * 构造函数，初始化Redis消息监听容器
     *
     * @param config             集群配置信息，包含Redis channel等配置
     * @param connectionFactory  Redis连接工厂，用于创建Redis连接
     * @param magicAPIService    MagicAPI服务实例，用于处理集群通知
     */
    public MagicAPIRedisMessageListenerContainer(ClusterConfig config,
                                                  RedisConnectionFactory connectionFactory,
                                                  MagicAPIService magicAPIService) {
        this.config = config;
        this.connectionFactory = connectionFactory;
        this.magicAPIService = magicAPIService;
    }

    /**
     * 初始化方法，在Bean构造完成后自动执行
     * 设置Redis连接工厂并注册消息监听器，监听指定的Redis频道以接收集群通知
     */
    @PostConstruct
    public void init() {
        String channel = config.getChannel();
        log.info("开启集群通知监听，Redis channel: {}", channel);
        setConnectionFactory(connectionFactory);
        addMessageListener((message, pattern) ->
                magicAPIService.processNotify(JsonUtils.readValue(message.getBody(), MagicNotify.class)),
                ChannelTopic.of(channel));
    }
}
