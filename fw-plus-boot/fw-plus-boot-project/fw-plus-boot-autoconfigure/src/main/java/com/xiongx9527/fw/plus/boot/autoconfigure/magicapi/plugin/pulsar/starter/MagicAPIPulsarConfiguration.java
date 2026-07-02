package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.DefaultPulsarConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.module.PulsarModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.web.MagicPulsarController;
import org.apache.pulsar.client.api.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * @author zzy
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.pulsar", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.apache.pulsar.client.api.Message"})
//@Configuration
@EnableConfigurationProperties(MagicPulsarConfig.class)
public class MagicAPIPulsarConfiguration implements MagicPluginConfiguration {

    /** Pulsar配置信息 */
    private final MagicPulsarConfig config;

    /**
     * 构造函数，初始化MagicAPIPulsarConfiguration实例
     *
     * @param config Pulsar配置信息
     */
    public MagicAPIPulsarConfiguration(MagicPulsarConfig config) {
        this.config = config;
    }


    /**
     * 创建Magic动态Pulsar数据源Bean
     * 当容器中不存在MagicDynamicPulsarDataSource Bean时创建实例
     *
     * @param pulsarConsumerService Pulsar消费者服务
     * @return MagicDynamicPulsarDataSource Magic动态Pulsar数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicPulsarDataSource magicDynamicPulsarDataSource(PulsarConsumerService pulsarConsumerService) {
        MagicDynamicPulsarDataSource magicDynamicPulsarDataSource = new MagicDynamicPulsarDataSource(pulsarConsumerService);
        return magicDynamicPulsarDataSource;
    }

    /**
     * 创建Pulsar数据源信息资源存储Bean
     * 当容器中不存在PulsarDataSourceInfoMagicResourceStorage Bean时创建默认实例
     *
     * @return PulsarDataSourceInfoMagicResourceStorage Pulsar数据源信息资源存储器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarDataSourceInfoMagicResourceStorage pulsarDataSourceInfoMagicResourceStorage() {
        return new PulsarDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Pulsar数据源动态注册Bean
     * 当容器中不存在PulsarDataSourceMagicDynamicRegistry Bean时创建实例
     *
     * @param pulsarDataSourceInfoMagicResourceStorage Pulsar数据源信息资源存储器
     * @param magicDynamicPulsarDataSource             Pulsar动态数据源
     * @return PulsarDataSourceMagicDynamicRegistry Pulsar数据源动态注册器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarDataSourceMagicDynamicRegistry pulsarDataSourceMagicDynamicRegistry(
            PulsarDataSourceInfoMagicResourceStorage pulsarDataSourceInfoMagicResourceStorage,
            MagicDynamicPulsarDataSource magicDynamicPulsarDataSource) {
        return new PulsarDataSourceMagicDynamicRegistry(pulsarDataSourceInfoMagicResourceStorage,
                magicDynamicPulsarDataSource);
    }

    /**
     * 创建Pulsar订阅信息资源存储Bean
     * 当容器中不存在PulsarSubscribeInfoMagicResourceStorage Bean时创建默认实例
     *
     * @return PulsarSubscribeInfoMagicResourceStorage Pulsar订阅信息资源存储器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarSubscribeInfoMagicResourceStorage pulsarSubscribeInfoMagicResourceStorage() {
        return new PulsarSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建Pulsar订阅动态注册Bean
     * 当容器中不存在PulsarSubscribeMagicDynamicRegistry Bean时创建实例，并依赖于pulsarDataSourceMagicDynamicRegistry
     *
     * @param pulsarSubscribeInfoMagicResourceStorage Pulsar订阅信息资源存储器
     * @param magicDynamicPulsarDataSource            Pulsar动态数据源
     * @return PulsarSubscribeMagicDynamicRegistry Pulsar订阅动态注册器
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn("pulsarDataSourceMagicDynamicRegistry")
    public PulsarSubscribeMagicDynamicRegistry pulsarSubscribeMagicDynamicRegistry(
            PulsarSubscribeInfoMagicResourceStorage pulsarSubscribeInfoMagicResourceStorage,
            MagicDynamicPulsarDataSource magicDynamicPulsarDataSource) {
        return new PulsarSubscribeMagicDynamicRegistry(pulsarSubscribeInfoMagicResourceStorage,
                magicDynamicPulsarDataSource);
    }

    /**
     * 创建Pulsar消费者服务Bean
     * 当容器中不存在PulsarConsumerService Bean时创建默认实现
     *
     * @return PulsarConsumerService 默认的Pulsar消费者服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarConsumerService pulsarConsumerService() {
        return new DefaultPulsarConsumerService();
    }

    /**
     * 创建Pulsar插件信息
     *
     * @return Plugin Pulsar插件对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("pulsar", "MagicPulsar", "magic-pulsar.1.0.0.iife.js");
    }

    /**
     * 注册MagicPulsar控制器
     *
     * @return MagicControllerRegister 控制器注册器，用于注册MagicPulsarController
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicPulsarController(configuration));
    }

    /**
     * 创建Pulsar模块Bean
     *
     * @param magicDynamicPulsarDataSource Pulsar动态数据源
     * @param producer                     Pulsar生产者（可选）
     * @return PulsarModule Pulsar模块实例
     */
    @Bean
    public PulsarModule pulsarModule(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource, @Autowired(required = false) Producer<byte[]> producer) {
        return new PulsarModule(magicDynamicPulsarDataSource,producer);
    }
}
