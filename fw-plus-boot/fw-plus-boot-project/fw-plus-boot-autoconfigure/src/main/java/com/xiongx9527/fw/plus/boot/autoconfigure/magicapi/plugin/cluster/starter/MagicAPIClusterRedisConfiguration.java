package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container.MagicAPIRedisMessageListenerContainer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.service.MagicNotifyService;
import org.ssssssss.magicapi.utils.JsonUtils;
import java.util.Arrays;

@Slf4j
@Configuration
@ConditionalOnBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
@EnableConfigurationProperties(ClusterConfig.class)
public class MagicAPIClusterRedisConfiguration {

    /**
     * 集群配置信息
     *
     * 该字段存储了当前集群的配置参数，用于指导集群的行为和设置
     */
    private final ClusterConfig config;

    /**
     * 构造函数，初始化集群配置
     * @param config 集群配置对象，用于配置Redis集群相关信息
     */
    public MagicAPIClusterRedisConfiguration(ClusterConfig config) {
        this.config = config;
    }

    /**
     * 创建MagicAPI Redis消息监听容器Bean
     * @return MagicAPIRedisMessageListenerContainer Redis消息监听容器实例
     */
    @Bean
    public MagicAPIRedisMessageListenerContainer magicAPIRedisMessageListenerContainer() {
        return new MagicAPIRedisMessageListenerContainer(this.config);
    }

    /**
     * 使用Redis推送通知
     * @param redisModule Redis模块，用于执行Redis命令
     * @return MagicNotifyService 通知服务实例，通过Redis的publish命令发送通知消息
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicNotifyService magicNotifyService(RedisModule redisModule) {
        return magicNotify -> redisModule.execute("publish", Arrays.asList(config.getChannel(), JsonUtils.toJsonString(magicNotify)));
    }

}
