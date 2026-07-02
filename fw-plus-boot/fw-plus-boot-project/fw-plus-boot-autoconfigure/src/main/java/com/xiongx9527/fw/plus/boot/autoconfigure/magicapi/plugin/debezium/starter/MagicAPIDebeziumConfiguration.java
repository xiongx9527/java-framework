package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.ClusterMagicDynamicDebeziumDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.MagicDynamicDebeziumDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.StandaloneMagicDynamicDebeziumDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.service.DebeziumDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.service.DebeziumDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.web.MagicDebeziumController;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.debezium", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"io.debezium.embedded.Connect","org.springframework.kafka.core.ProducerFactory"})
@Configuration
public class MagicAPIDebeziumConfiguration implements MagicPluginConfiguration {


    /**
     * 创建集群模式的Debezium数据源管理器实例
     * 当系统中存在StringRedisTemplate类时启用，表示使用集群模式
     *
     * @return MagicDynamicDebeziumDataSource 集群模式的Debezium数据源管理器实例
     */
    @Bean
    @ConditionalOnBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public MagicDynamicDebeziumDataSource clusterMagicDynamicDebeziumDataSource() {
        return new ClusterMagicDynamicDebeziumDataSource();
    }

    /**
     * 创建独立模式的Debezium数据源管理器实例
     * 当系统中不存在StringRedisTemplate类时启用，表示使用独立模式而非集群模式
     *
     * @return MagicDynamicDebeziumDataSource 独立模式的Debezium数据源管理器实例
     */
    @Bean
    @ConditionalOnMissingBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public MagicDynamicDebeziumDataSource standaloneMagicDynamicDebeziumDataSource() {
        return new StandaloneMagicDynamicDebeziumDataSource();
    }


    /**
     * 创建Debezium数据源信息存储Bean实例
     * 用于存储和管理Debezium数据源的配置信息
     *
     * @return DebeziumDataSourceInfoMagicResourceStorage Debezium数据源信息存储实例
     */
    @Bean
    public DebeziumDataSourceInfoMagicResourceStorage debeziumDataSourceInfoMagicResourceStorage() {
        return new DebeziumDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Debezium数据源动态注册Bean实例
     * 用于动态注册和管理Debezium数据源
     *
     * @param debeziumDataSourceInfoMagicResourceStorage Debezium数据源信息存储实例
     * @param magicDynamicDebeziumDataSource Debezium数据源管理器实例
     * @return DebeziumDataSourceMagicDynamicRegistry Debezium数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DebeziumDataSourceMagicDynamicRegistry debeziumDataSourceMagicDynamicRegistry(
            DebeziumDataSourceInfoMagicResourceStorage debeziumDataSourceInfoMagicResourceStorage,
            MagicDynamicDebeziumDataSource magicDynamicDebeziumDataSource) {
        return new DebeziumDataSourceMagicDynamicRegistry(debeziumDataSourceInfoMagicResourceStorage,
                magicDynamicDebeziumDataSource);
    }


    /**
     * 定义插件信息
     * 返回当前Debezium插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("debezium", "MagicDebezium", "magic-debezium.1.0.0.iife.js");
    }

    /**
     * 获取控制器注册器
     *
     * @return 控制器注册器，用于注册MagicDebeziumController控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        // 创建并返回一个控制器注册器，该注册器将MagicDebeziumController注册到映射中
        return (mapping, configuration) -> mapping.registerController(new MagicDebeziumController(configuration));
    }


}
