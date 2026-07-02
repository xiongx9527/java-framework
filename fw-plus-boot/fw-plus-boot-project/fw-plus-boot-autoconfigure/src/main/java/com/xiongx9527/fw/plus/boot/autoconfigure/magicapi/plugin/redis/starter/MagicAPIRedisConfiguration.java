package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service.RedisDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service.RedisDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.web.MagicRedisController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.config.Resource;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.redis.RedisResource;

@ConditionalOnProperty(prefix = "magic-api.plugin.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate","org.apache.commons.pool2.impl.GenericObjectPoolConfig",
        "org.ssssssss.magicapi.redis.RedisModule"})
@Configuration
public class MagicAPIRedisConfiguration implements MagicPluginConfiguration {

    /**
     * MagicAPIRedisConfiguration 构造函数
     *
     * @param properties MagicAPI的配置属性对象，用于初始化Redis相关配置
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于创建MagicAPIRedisConfiguration实例
     *
     * @param properties MagicAPI的配置属性对象，包含Redis相关的配置信息
     */
    public MagicAPIRedisConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 使用Redis存储
     */
    @Primary
    @Bean
    @ConditionalOnProperty(prefix = "magic-api", name = "resource.type", havingValue = "redis")
    public org.ssssssss.magicapi.core.resource.Resource magicRedisResource(RedisConnectionFactory connectionFactory) {
        Resource resource = properties.getResource();
        return new RedisResource(new StringRedisTemplate(connectionFactory), resource.getPrefix(), resource.isReadonly());
    }


    /**
     * 创建MagicDynamicRedisDataSource实例
     * 用于动态管理Redis数据源连接，支持多数据源的动态创建和销毁
     *
     * @return MagicDynamicRedisDataSource Redis数据源管理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicRedisDataSource magicDynamicRedisDataSource() {
        MagicDynamicRedisDataSource magicDynamicRedisDataSource = new MagicDynamicRedisDataSource();
        return magicDynamicRedisDataSource;
    }

    /**
     * 注入redis模块
     */
    @Primary
    @Bean
    @DependsOn(value={"magicDynamicRedisDataSource"})
    public RedisModule redisModule(MagicDynamicRedisDataSource magicDynamicRedisDataSource,@Autowired(required =false) RedisConnectionFactory connectionFactory) {
        return new RedisModule(magicDynamicRedisDataSource,connectionFactory);
    }

    /**
     * 创建Redis数据源信息存储Bean实例
     * 用于存储和管理Redis数据源的配置信息
     *
     * @return RedisDataSourceInfoMagicResourceStorage Redis数据源信息存储实例
     */
    @Bean
    public RedisDataSourceInfoMagicResourceStorage redisDataSourceInfoMagicResourceStorage() {
        return new RedisDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Redis数据源动态注册Bean实例
     * 用于动态注册和管理Debezium数据源
     *
     * @param redisDataSourceInfoMagicResourceStorage Debezium数据源信息存储实例
     * @param magicDynamicRedisDataSource Debezium数据源管理器实例
     * @return RedisDataSourceMagicDynamicRegistry Redis数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisDataSourceMagicDynamicRegistry redisDataSourceMagicDynamicRegistry(RedisDataSourceInfoMagicResourceStorage redisDataSourceInfoMagicResourceStorage,
            MagicDynamicRedisDataSource magicDynamicRedisDataSource) {
        return new RedisDataSourceMagicDynamicRegistry(redisDataSourceInfoMagicResourceStorage,
                magicDynamicRedisDataSource);
    }


    /**
     * 定义插件信息
     * 返回当前Redis插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("redis", "MagicRedis", "magic-redis.1.0.0.iife.js");
    }


    /**
     * 控制器注册方法，用于创建并注册MagicRedisController实例
     *
     * @return 返回一个MagicControllerRegister接口的实现，该实现负责控制器的注册逻辑
     *         该实现接收mapping和configuration两个参数：
     *         - mapping: 用于注册控制器的映射对象
     *         - configuration: 配置对象，用于初始化MagicRedisController
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        // 返回一个Lambda表达式，实现MagicControllerRegister接口
        // 该表达式创建新的MagicRedisController实例并将其注册到mapping中
        return (mapping, configuration) -> mapping.registerController(new MagicRedisController(configuration));
    }


}
