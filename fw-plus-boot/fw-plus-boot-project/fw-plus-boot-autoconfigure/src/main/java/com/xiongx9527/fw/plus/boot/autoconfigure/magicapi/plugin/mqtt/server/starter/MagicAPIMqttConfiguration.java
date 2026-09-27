package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.ClusterMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.StandaloneMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.module.MqttModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service.MqttDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service.MqttDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service.MqttSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service.MqttSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.web.MagicMqttController;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@Slf4j
// 内置 MQTT 服务端会监听端口并承担 Broker 职责；普通外连 Broker 场景请使用 mqttclient 插件。
@ConditionalOnProperty(prefix = "magic-api.plugin.mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(name = {"net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate"})
@EnableConfigurationProperties(MagicAPIMqttConfig.class)
@Configuration
public class MagicAPIMqttConfiguration implements MagicPluginConfiguration {

    /** MagicAPI配置属性 */
    private final MagicAPIProperties properties;

    /**
     * 构造函数
     * 
     * @param properties MagicAPI配置属性
     */
    public MagicAPIMqttConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建一个独立模式的 MQTT 数据源实例。
     * 该 Bean 仅在 Spring 容器中不存在 RedisTemplate 类型的 Bean 时才会被创建。
     *
     * @return 返回 StandaloneMagicDynamicMqttDataSource 实例，作为基础 MQTT 数据源。
     */
    @Bean(value = "magicDynamicMqttDataSource")
    @ConditionalOnMissingBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public BaseMagicDynamicMqttDataSource standaloneMagicDynamicMqttDataSource() {
        return new StandaloneMagicDynamicMqttDataSource();
    }

    /**
     * 创建一个集群模式的 MQTT 数据源实例。
     * 该 Bean 仅在 Spring 容器中存在 RedisTemplate 类型的 Bean 时才会被创建。
     *
     * @return 返回 ClusterMagicDynamicMqttDataSource 实例，作为集群模式下的 MQTT 数据源。
     */
    @Bean(value = "magicDynamicMqttDataSource")
    @ConditionalOnBean(type = {"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule"})
    public BaseMagicDynamicMqttDataSource clusterMagicDynamicMqttDataSource() {
        return new ClusterMagicDynamicMqttDataSource();
    }

    /**
     * 创建并初始化 MqttModule 模块。
     * 该模块依赖于已定义的 MQTT 数据源，并可选地注入 MqttServer 实例。
     * 此 Bean 的初始化依赖于 magicDynamicMqttDataSource 已完成初始化。
     *
     * @param magicDynamicWeaviateDataSource MQTT 数据源实例。
     * @param mqttServer                     可选的 MQTT 服务端实例。
     * @return 返回初始化后的 MqttModule 实例。
     */
    @Bean
    @DependsOn(value={"magicDynamicMqttDataSource"})
    public MqttModule mqttModule(BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource,
                                 @Autowired(required = false) MqttServer mqttServer) {
        return new MqttModule(magicDynamicMqttDataSource, mqttServer);
    }

    /**
     * 创建用于存储 MQTT 数据源信息的资源存储实例。
     *
     * @return 返回 MqttDataSourceInfoMagicResourceStorage 实例。
     */
    @Bean
    public MqttDataSourceInfoMagicResourceStorage mqttDataSourceInfoMagicResourceStorage() {
        return new MqttDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建 MQTT 数据源动态注册器实例。
     * 若容器中已存在同类型的 Bean，则不会重复创建。
     * 该注册器依赖于数据源信息存储和基础 MQTT 数据源。
     *
     * @param mqttDataSourceInfoMagicResourceStorage 数据源信息存储实例。
     * @param magicDynamicMqttDataSource             基础 MQTT 数据源实例。
     * @return 返回 MqttDataSourceMagicDynamicRegistry 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttDataSourceMagicDynamicRegistry mqttDataSourceMagicDynamicRegistry(MqttDataSourceInfoMagicResourceStorage mqttDataSourceInfoMagicResourceStorage, BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource) {
        return new MqttDataSourceMagicDynamicRegistry(mqttDataSourceInfoMagicResourceStorage,
                magicDynamicMqttDataSource);
    }

    /**
     * 创建用于存储 MQTT 订阅信息的资源存储实例。
     *
     * @return 返回 MqttSubscribeInfoMagicResourceStorage 实例。
     */
    @Bean
    public MqttSubscribeInfoMagicResourceStorage mqttSubscribeInfoMagicResourceStorage() {
        return new MqttSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建 MQTT 订阅信息动态注册器实例。
     * 若容器中已存在同类型的 Bean，则不会重复创建。
     * 该注册器依赖于订阅信息存储实例。
     *
     * @param mqttSubscribeInfoMagicResourceStorage 订阅信息存储实例。
     * @return 返回 MqttSubscribeMagicDynamicRegistry 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttSubscribeMagicDynamicRegistry mqttSubscribeMagicDynamicRegistry(MqttSubscribeInfoMagicResourceStorage mqttSubscribeInfoMagicResourceStorage) {
        return new MqttSubscribeMagicDynamicRegistry(mqttSubscribeInfoMagicResourceStorage);
    }

    /**
     * 返回当前插件的基本信息。
     * 包括插件名称、显示名称以及前端资源文件路径。
     *
     * @return 返回 Plugin 实例，包含插件元数据。
     */
    @Override
    public Plugin plugin() {
        return new Plugin("mqtt", "MagicMqtt", "magic-mqtt.1.0.0.iife.js");
    }

    /**
     * 提供控制器注册逻辑。
     * 使用给定的映射和配置对象注册 MagicMqttController 控制器。
     *
     * @return 返回 MagicControllerRegister 实例，用于注册控制器。
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicMqttController(configuration));
    }


}
