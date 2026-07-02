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

@ConditionalOnProperty(prefix = "magic-api.plugin.cluster", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
@Configuration
@Import(MagicAPIClusterRedisConfiguration.class)
public class MagicAPIClusterConfiguration implements MagicPluginConfiguration {

    /**
     * MagicAPI配置属性对象
     *
     * 该字段存储了MagicAPI的所有配置属性，包括接口路径、数据源配置、安全认证等相关设置
     * 通过该属性对象可以获取和修改MagicAPI的各项配置参数
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于创建MagicAPIClusterConfiguration实例
     * @param properties MagicAPI的配置属性对象
     */
    public MagicAPIClusterConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建并返回集群插件实例
     * @return Plugin 集群插件对象，插件名称为"cluster"
     */
    @Override
    public Plugin plugin() {
        return new Plugin("cluster");
    }

    /**
     * 消息处理服务
     * 创建MagicSynchronizationService实例，用于处理集群环境下的数据同步
     * @param magicNotifyService 通知服务实例
     * @return MagicSynchronizationService 同步服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicSynchronizationService magicSynchronizationService(MagicNotifyService magicNotifyService) {
        return new MagicSynchronizationService(magicNotifyService, properties.getInstanceId());
    }

}
