package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.DefaultKafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.module.KafkaModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.web.MagicKafkaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author zzy
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.springframework.kafka.core.ProducerFactory","org.springframework.kafka.core.ConsumerFactory"})
@Configuration
@EnableConfigurationProperties(MagicKafkaConfig.class)
public class MagicAPIKafkaConfiguration implements MagicPluginConfiguration {

    /** Kafka配置信息 */
    private final MagicKafkaConfig config;

    /**
     * 构造函数，初始化MagicAPIKafkaConfiguration实例
     *
     * @param config Kafka配置信息
     */
    public MagicAPIKafkaConfiguration(MagicKafkaConfig config) {
        this.config = config;
    }


    /**
     * 创建并配置一个MagicDynamicKafkaDataSource bean
     * 该方法在没有检测到现有Bean定义时，会条件性地创建一个Bean
     * 主要用于动态地与Kafka进行交互，结合生产者和消费者服务
     *
     * @param kafkaConsumerService Kafka消费者服务，用于从Kafka接收消息
     * @return 返回一个配置好的MagicDynamicKafkaDataSource实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicKafkaDataSource magicDynamicKafkaDataSource(KafkaConsumerService kafkaConsumerService) {
        // 实例化MagicDynamicKafkaDataSource，传入必要服务
        MagicDynamicKafkaDataSource magicDynamicKafkaDataSource = new MagicDynamicKafkaDataSource(kafkaConsumerService);
        // 返回配置好的数据源实例
        return magicDynamicKafkaDataSource;
    }

    /**
     * 创建KafkaDataSourceInfoMagicResourceStorage的Bean实例
     * 该方法利用Spring框架的@Bean注解来定义一个Bean，使得KafkaDataSourceInfoMagicResourceStorage可以在Spring应用中被自动扫描和注入
     * @ConditionalOnMissingBean注解确保只有在当前上下文中不存在相同类型的Bean时，才会创建一个新的KafkaDataSourceInfoMagicResourceStorage实例
     * 这有助于防止Bean的重复创建，并确保应用的配置具有灵活性和可扩展性
     *
     * @return 返回KafkaDataSourceInfoMagicResourceStorage的一个实例，用于处理Kafka数据源信息的魔法资源存储
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage() {
        return new KafkaDataSourceInfoMagicResourceStorage();
    }

    /**
     * 注册Kafka数据源动态注册中心
     * 该方法通过Spring的条件注解来条件性地创建并注册一个KafkaDataSourceMagicDynamicRegistry对象
     * 主要用于在应用程序上下文中动态管理Kafka数据源配置
     *
     * @param kafkaDataSourceInfoMagicResourceStorage Kafka数据源信息资源存储对象，用于存储和管理Kafka数据源的相关信息
     * @param magicDynamicKafkaDataSource 动态Kafka数据源对象，用于实际的数据源动态管理与切换
     * @return 返回一个KafkaDataSourceMagicDynamicRegistry对象，用于动态注册和管理Kafka数据源
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaDataSourceMagicDynamicRegistry kafkaDataSourceMagicDynamicRegistry(
            KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage,
            MagicDynamicKafkaDataSource magicDynamicKafkaDataSource) {
        return new KafkaDataSourceMagicDynamicRegistry(kafkaDataSourceInfoMagicResourceStorage,
                magicDynamicKafkaDataSource);
    }

    /**
     * 创建Kafka订阅信息的Magic资源存储 bean
     *
     * 此方法使用Spring框架的@Bean注解来定义一个bean，同时使用@ConditionalOnMissingBean注解来确保只有在当前上下文中
     * 尚不存在相同类型的bean时，才会创建这个bean这样做可以避免bean的重复创建，确保了应用的性能和资源的有效利用
     *
     * @return KafkaSubscribeInfoMagicResourceStorage的实例，用于处理Kafka订阅信息的Magic资源存储
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeInfoMagicResourceStorage() {
        return new KafkaSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建Kafka订阅魔法动态注册 bean
     * 该方法依赖于 kafkaDataSourceMagicDynamicRegistry bean 的存在
     * 如果容器中没有 KafkaSubscribeMagicDynamicRegistry bean，则创建一个
     *
     * @param kafkaSubscribeInfoMagicResourceStorage Kafka订阅信息魔法资源存储，用于存储订阅信息
     * @param magicDynamicKafkaDataSource 魔法动态Kafka数据源，用于提供数据源支持
     * @return 返回一个KafkaSubscribeMagicDynamicRegistry实例，用于动态注册和管理Kafka订阅信息
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn("kafkaDataSourceMagicDynamicRegistry")
    public KafkaSubscribeMagicDynamicRegistry kafkaSubscribeMagicDynamicRegistry(
            KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeInfoMagicResourceStorage,
            MagicDynamicKafkaDataSource magicDynamicKafkaDataSource) {
        return new KafkaSubscribeMagicDynamicRegistry(kafkaSubscribeInfoMagicResourceStorage,
                magicDynamicKafkaDataSource);
    }

    /**
     * 创建并配置KafkaConsumerService的Bean
     * 该方法利用Spring框架的@Bean注解来声明一个Bean，使用@ConditionalOnMissingBean注解来确保只有在容器中没有其他同类型Bean的情况下才创建该Bean
     * 这样做可以避免Bean的重复创建，同时提供了一种默认实现，即当用户没有提供自定义实现时，使用DefaultKafkaConsumerService作为默认实现
     *
     * @return KafkaConsumerService的实例，负责处理Kafka消息消费的具体逻辑
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerService kafkaConsumerService() {
        return new DefaultKafkaConsumerService();
    }

    /**
     * 获取当前配置的插件实例
     *
     * 此方法用于创建并返回一个Plugin对象，该对象代表了要使用的具体消息队列插件
     * 它指定了插件的名称、标识符和脚本文件路径，以便在系统中动态加载和使用该插件
     *
     * @return Plugin 实例，包含插件的名称、标识符和脚本文件路径
     */
    @Override
    public Plugin plugin() {
        // 创建并返回一个Plugin对象，参数分别为插件名称、标识符和脚本文件路径
        // 这里配置的是一个名为"kafka消息队列"的插件，其标识符为"MagicKafka"，脚本文件路径为"magic-kafka.1.0.0.iife.js"
        return new Plugin("kafka", "MagicKafka", "magic-kafka.1.0.0.iife.js");
    }

    /**
     * 重写获取控制器注册器的方法
     *
     * 此方法返回一个控制器注册器，用于在给定的映射和配置下注册控制器
     * 它特别适用于注册MagicKafkaController，该控制器是为处理Kafka相关操作而设计的
     *
     * @return MagicControllerRegister 控制器注册器接口的实现，用于注册MagicKafkaController
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicKafkaController(configuration));
    }

    /**
     * 创建Kafka模块Bean
     *
     * @param magicDynamicKafkaDataSource Kafka动态数据源
     * @param kafkaTemplate               Kafka模板（可选）
     * @return KafkaModule Kafka模块实例
     */
    @Bean
    public KafkaModule kafkaModule(MagicDynamicKafkaDataSource magicDynamicKafkaDataSource,@Autowired(required = false) KafkaTemplate kafkaTemplate) {
        return new KafkaModule(magicDynamicKafkaDataSource,kafkaTemplate);
    }


}
