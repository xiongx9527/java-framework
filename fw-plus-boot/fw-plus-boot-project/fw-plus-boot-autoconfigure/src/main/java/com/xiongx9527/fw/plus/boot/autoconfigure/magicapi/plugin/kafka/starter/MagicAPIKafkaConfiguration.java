package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.DefaultKafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.module.KafkaModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.web.MagicKafkaController;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaTemplate;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * @author zzy
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.springframework.kafka.core.ProducerFactory","org.springframework.kafka.core.ConsumerFactory","org.apache.kafka.clients.admin.AdminClient"})
@Configuration
@EnableConfigurationProperties(MagicKafkaConfig.class)
public class MagicAPIKafkaConfiguration implements MagicPluginConfiguration {

    /**
     * 创建Kafka动态数据源
     *
     * @param kafkaConsumerService Kafka消费者服务
     * @return MagicDynamicKafkaDataSource实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicKafkaDataSource magicDynamicKafkaDataSource(KafkaConsumerService kafkaConsumerService) {
        return new MagicDynamicKafkaDataSource(kafkaConsumerService);
    }

    /**
     * 创建Kafka数据源信息资源存储器
     *
     * @return KafkaDataSourceInfoMagicResourceStorage实例
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage() {
        return new KafkaDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Kafka数据源动态注册器
     *
     * @param kafkaDataSourceInfoMagicResourceStorage 数据源信息资源存储器
     * @param magicDynamicKafkaDataSource             Kafka动态数据源
     * @return KafkaDataSourceMagicDynamicRegistry实例
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
     * 创建Kafka订阅信息资源存储器
     *
     * @return KafkaSubscribeInfoMagicResourceStorage实例
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeInfoMagicResourceStorage() {
        return new KafkaSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建Kafka订阅动态注册器
     *
     * @param kafkaSubscribeInfoMagicResourceStorage  订阅信息资源存储器
     * @param magicDynamicKafkaDataSource             Kafka动态数据源
     * @param kafkaDataSourceInfoMagicResourceStorage 数据源信息存储器（用于校验数据源是否存在）
     * @return KafkaSubscribeMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn("kafkaDataSourceMagicDynamicRegistry")
    public KafkaSubscribeMagicDynamicRegistry kafkaSubscribeMagicDynamicRegistry(
            KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeInfoMagicResourceStorage,
            MagicDynamicKafkaDataSource magicDynamicKafkaDataSource,
            KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage) {
        return new KafkaSubscribeMagicDynamicRegistry(kafkaSubscribeInfoMagicResourceStorage,
                magicDynamicKafkaDataSource, kafkaDataSourceInfoMagicResourceStorage, kafkaSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建Kafka消费者服务
     *
     * @return KafkaConsumerService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerService kafkaConsumerService() {
        return new DefaultKafkaConsumerService();
    }

    @Override
    public Plugin plugin() {
        Plugin plugin = new Plugin("kafka", "MagicKafka", "magic-kafka.1.0.0.iife.js");
        String adminEnabled = SpringUtils.getAppConfig("magic-api.plugin.kafka.admin.enabled", "false");
        plugin.setAdminEnabled(Boolean.parseBoolean(adminEnabled));
        return plugin;
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicKafkaController(configuration));
    }

    /**
     * 创建Kafka模块，注册为Magic-API模块
     *
     * @param magicDynamicKafkaDataSource Kafka动态数据源
     * @param kafkaTemplate               Kafka模板实例（可选）
     * @return KafkaModule实例
     */
    @Bean
    public KafkaModule kafkaModule(MagicDynamicKafkaDataSource magicDynamicKafkaDataSource, @Autowired(required = false) KafkaTemplate kafkaTemplate) {
        return new KafkaModule(magicDynamicKafkaDataSource, kafkaTemplate);
    }

}
