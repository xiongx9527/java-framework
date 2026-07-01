package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container.MagicAPIRedisMessageListenerContainer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.core.service.MagicNotifyService;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Arrays;

/**
 * MagicAPI集群Redis配置类
 * 启用ClusterConfig配置属性，并注册基于Redis的集群通信相关Bean
 */
@Configuration
@EnableConfigurationProperties(ClusterConfig.class)
public class MagicAPIClusterRedisConfiguration {

    /**
     * 集群配置信息
     */
    private final ClusterConfig config;

    /**
     * 构造函数，注入集群配置
     *
     * @param config 集群配置信息，包含Redis channel等配置
     */
    public MagicAPIClusterRedisConfiguration(ClusterConfig config) {
        this.config = config;
    }

    /**
     * 创建Redis消息监听容器Bean
     * 用于监听Redis频道中的集群通知消息
     *
     * @param connectionFactory Redis连接工厂，用于建立Redis连接
     * @param magicAPIService   MagicAPI服务实例，用于处理接收到的通知
     * @return Redis消息监听容器实例
     */
    @Bean
    public MagicAPIRedisMessageListenerContainer magicAPIRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, MagicAPIService magicAPIService) {
        return new MagicAPIRedisMessageListenerContainer(config, connectionFactory, magicAPIService);
    }

    /**
     * 创建MagicAPI通知服务Bean
     * 仅在容器中不存在该类型Bean时创建
     * 通过Redis的publish命令向集群广播通知消息
     *
     * @param redisModule Redis模块，用于执行Redis命令
     * @return 通知服务实例，负责将通知发布到Redis频道
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicNotifyService magicNotifyService(RedisModule redisModule) {
        return magicNotify -> redisModule.execute("publish",
                Arrays.asList(config.getChannel(), JsonUtils.toJsonString(magicNotify)));
    }
}
