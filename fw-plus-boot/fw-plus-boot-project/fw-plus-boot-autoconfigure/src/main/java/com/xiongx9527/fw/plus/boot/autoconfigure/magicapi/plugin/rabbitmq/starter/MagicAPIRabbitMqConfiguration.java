package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.DefaultRabbitMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.RabbitMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module.RabbitMqModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.web.MagicRabbitMqController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@ConditionalOnProperty(prefix = "magic-api.plugin.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.springframework.amqp.rabbit.core.RabbitTemplate"})
@Configuration
@EnableConfigurationProperties({MagicRabbitMqConfig.class})
public class MagicAPIRabbitMqConfiguration implements MagicPluginConfiguration {

    /**
     * MagicRabbitMqConfig配置对象，用于存储RabbitMQ相关的配置信息
     */
    private final MagicRabbitMqConfig config;

    /**
     * 构造函数，用于创建MagicAPIRabbitMqConfiguration实例
     *
     * @param config RabbitMQ配置对象，包含连接信息、队列配置等参数
     */
    public MagicAPIRabbitMqConfiguration(MagicRabbitMqConfig config) {
        this.config = config;
    }


    /**
     * 创建并配置一个 MagicDynamicRabbitMqDataSource 实例。
     * <p>
     * 此 Bean 在当前上下文中不存在同名 Bean 时才会被创建。
     *
     * @param rabbitMqConsumerService RabbitMQ 消费者服务实例，用于处理消息消费逻辑
     * @return 返回一个新的 MagicDynamicRabbitMqDataSource 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource(RabbitMqConsumerService rabbitMqConsumerService) {
        MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource = new MagicDynamicRabbitMqDataSource(rabbitMqConsumerService);
        return magicDynamicRabbitMqDataSource;
    }

    /**
     * 创建并注册一个 RabbitMqDataSourceInfoMagicResourceStorage 实例。
     * <p>
     * 此 Bean 负责存储 RabbitMQ 数据源信息资源，在没有相同类型 Bean 存在时才进行初始化。
     *
     * @return 返回一个新的 RabbitMqDataSourceInfoMagicResourceStorage 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitMqDataSourceInfoMagicResourceStorage rabbitMqDataSourceInfoMagicResourceStorage() {
        return new RabbitMqDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建并注册 RabbitMqDataSourceMagicDynamicRegistry 实例。
     * <p>
     * 该 Bean 依赖于数据源信息存储和动态数据源，并负责将 RabbitMQ 数据源注册到系统中。
     *
     * @param rabbitMqDataSourceInfoMagicResourceStorage 数据源信息存储组件
     * @param magicDynamicRabbitMqDataSource 动态 RabbitMQ 数据源组件
     * @return 返回一个新的 RabbitMqDataSourceMagicDynamicRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitMqDataSourceMagicDynamicRegistry rabbitMqDataSourceMagicDynamicRegistry(RabbitMqDataSourceInfoMagicResourceStorage rabbitMqDataSourceInfoMagicResourceStorage, MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        return new RabbitMqDataSourceMagicDynamicRegistry(rabbitMqDataSourceInfoMagicResourceStorage, magicDynamicRabbitMqDataSource);
    }

    /**
     * 创建并注册 RabbitMqSubscribeInfoMagicResourceStorage 实例。
     * <p>
     * 该 Bean 用于管理 RabbitMQ 订阅相关信息的持久化或内存存储。
     *
     * @return 返回一个新的 RabbitMqSubscribeInfoMagicResourceStorage 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitMqSubscribeInfoMagicResourceStorage rabbitMqSubscribeInfoMagicResourceStorage() {
        return new RabbitMqSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建并注册 RabbitMqSubscribeMagicDynamicRegistry 实例。
     * <p>
     * 该 Bean 依赖于订阅信息存储与动态数据源，并确保在 rabbitMqDataSourceMagicDynamicRegistry 初始化之后加载。
     *
     * @param rabbitMqSubscribeInfoMagicResourceStorage 订阅信息存储组件
     * @param magicDynamicRabbitMqDataSource 动态 RabbitMQ 数据源组件
     * @return 返回一个新的 RabbitMqSubscribeMagicDynamicRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn({"rabbitMqDataSourceMagicDynamicRegistry"})
    public RabbitMqSubscribeMagicDynamicRegistry rabbitMqSubscribeMagicDynamicRegistry(RabbitMqSubscribeInfoMagicResourceStorage rabbitMqSubscribeInfoMagicResourceStorage, MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        return new RabbitMqSubscribeMagicDynamicRegistry(rabbitMqSubscribeInfoMagicResourceStorage, magicDynamicRabbitMqDataSource);
    }

    /**
     * 创建默认的 RabbitMQ 消费者服务实现。
     * <p>
     * 若 Spring 上下文中尚未存在 RabbitMqConsumerService 类型的 Bean，则会使用此方法创建一个默认实现。
     *
     * @return 返回一个新的 DefaultRabbitMqConsumerService 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RabbitMqConsumerService rabbitMqConsumerService() {
        return new DefaultRabbitMqConsumerService();
    }

    /**
     * 构造插件对象，描述当前模块的基本信息。
     * <p>
     * 包含插件名称、显示名称以及对应的 JavaScript 文件路径。
     *
     * @return 返回一个新的 Plugin 对象
     */
    public Plugin plugin() {
        return new Plugin("rabbitmq", "MagicRabbitMq", "magic-rabbitmq.1.0.0.iife.js");
    }

    /**
     * 创建并注册 RabbitMqModule 实例。
     * <p>
     * 该模块整合了生产者（可选）和动态数据源功能，提供统一的消息发送能力。
     *
     * @param producer 可选的 RabbitTemplate 生产者实例
     * @param magicDynamicRabbitMqDataSource 动态 RabbitMQ 数据源组件
     * @return 返回一个新的 RabbitMqModule 实例
     */
    @Bean
    public RabbitMqModule rabbitMqModule(@Autowired(required = false)  org.springframework.amqp.rabbit.core.RabbitTemplate producer , MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        return new RabbitMqModule(new RabbitMqProducer(producer),magicDynamicRabbitMqDataSource);
    }

    /**
     * 提供控制器注册器，用于向系统注册 MagicRabbitMqController 控制器。
     * <p>
     * 注册过程通过传入的 mapping 和 configuration 参数完成。
     *
     * @return 返回一个新的 MagicControllerRegister 实例
     */
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> {
            mapping.registerController(new MagicRabbitMqController(configuration));
        };
    }


}
