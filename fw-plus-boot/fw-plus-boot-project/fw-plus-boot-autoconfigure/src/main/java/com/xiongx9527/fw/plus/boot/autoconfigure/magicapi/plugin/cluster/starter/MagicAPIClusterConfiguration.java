package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service.MagicSynchronizationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicNotifyService;

/**
 * MagicAPI集群插件配置类
 * 当启用集群模式（magic-api.plugin.cluster.enabled=true）且Redis模块存在时自动装配
 * 负责初始化集群同步相关的Bean和配置
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.cluster", name = "enabled", havingValue = "true")
@ConditionalOnBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
@Configuration
@Import(MagicAPIClusterRedisConfiguration.class)
public class MagicAPIClusterConfiguration implements MagicPluginConfiguration {

    /**
     * MagicAPI配置属性
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，注入MagicAPI配置属性
     *
     * @param properties MagicAPI全局配置属性
     */
    public MagicAPIClusterConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册集群插件标识
     *
     * @return 集群插件对象，名称为"cluster"
     */
    @Override
    public Plugin plugin() {
        return new Plugin("cluster");
    }

    /**
     * 创建集群同步服务Bean
     * 仅在容器中不存在该类型Bean时创建
     *
     * @param magicNotifyService MagicAPI通知服务，用于发送集群通知
     * @return 集群同步服务实例，负责监听本地事件并广播到集群
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicSynchronizationService magicSynchronizationService(MagicNotifyService magicNotifyService) {
        return new MagicSynchronizationService(magicNotifyService, properties.getInstanceId());
    }
}
