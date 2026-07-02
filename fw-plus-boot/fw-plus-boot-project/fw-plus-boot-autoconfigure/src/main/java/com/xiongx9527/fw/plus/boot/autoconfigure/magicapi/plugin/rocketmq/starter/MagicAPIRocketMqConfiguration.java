package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.DefaultRocketMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.module.RocketMqModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.web.MagicRocketMqController;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
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

/**
 * @author zzy
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.apache.rocketmq.client.producer.DefaultMQProducer"})
@Configuration
@EnableConfigurationProperties(MagicRocketMqConfig.class)
public class MagicAPIRocketMqConfiguration implements MagicPluginConfiguration {

    private final MagicRocketMqConfig config;

    public MagicAPIRocketMqConfiguration(MagicRocketMqConfig config) {
        this.config = config;
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource(RocketMqConsumerService rocketMqConsumerService) {
        MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource = new MagicDynamicRocketMqDataSource(rocketMqConsumerService);
        return magicDynamicRocketMqDataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMqDataSourceInfoMagicResourceStorage rocketMqDataSourceInfoMagicResourceStorage() {
        return new RocketMqDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMqDataSourceMagicDynamicRegistry rocketMqDataSourceMagicDynamicRegistry(
            RocketMqDataSourceInfoMagicResourceStorage rocketMqDataSourceInfoMagicResourceStorage,
            MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        return new RocketMqDataSourceMagicDynamicRegistry(rocketMqDataSourceInfoMagicResourceStorage,
                magicDynamicRocketMqDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMqSubscribeInfoMagicResourceStorage rocketMqSubscribeInfoMagicResourceStorage() {
        return new RocketMqSubscribeInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn("rocketMqDataSourceMagicDynamicRegistry")
    public RocketMqSubscribeMagicDynamicRegistry rocketMqSubscribeMagicDynamicRegistry(
            RocketMqSubscribeInfoMagicResourceStorage rocketMqSubscribeInfoMagicResourceStorage,
            MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        return new RocketMqSubscribeMagicDynamicRegistry(rocketMqSubscribeInfoMagicResourceStorage,
                magicDynamicRocketMqDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMqConsumerService rocketMqConsumerService() {
        return new DefaultRocketMqConsumerService();
    }

    @Override
    public Plugin plugin() {
        return new Plugin("rocketmq", "MagicRocketMq", "magic-rocketmq.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicRocketMqController(configuration));
    }

    @Bean
    public RocketMqModule rocketMqModule(MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource,@Autowired(required = false)  DefaultMQProducer producer) {
        return new RocketMqModule(new RocketMqProducer(producer),magicDynamicRocketMqDataSource);
    }

}
