
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.DefaultMqttClientConsumerService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumerService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.module.MqttClientModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service.MqttClientDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service.MqttClientDataSourceMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service.MqttClientSubscribeInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service.MqttClientSubscribeMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.web.MagicMqttClientController;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.mqttclient", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate"})
@Configuration
@EnableConfigurationProperties(MagicAPIMqttClientConfig.class)
public class MagicAPIMqttClientConfiguration implements MagicPluginConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource(MqttClientConsumerService mqttClientConsumerService) {
        return new MagicDynamicMqttClientDataSource(mqttClientConsumerService);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage(MagicResourceService magicResourceService) {
        MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage = new MqttClientDataSourceInfoMagicResourceStorage();
        mqttClientDataSourceInfoMagicResourceStorage.setMagicResourceService(magicResourceService);
        return mqttClientDataSourceInfoMagicResourceStorage;
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClientDataSourceMagicDynamicRegistry mqttClientDataSourceMagicDynamicRegistry(
            MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage,
            MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        return new MqttClientDataSourceMagicDynamicRegistry(mqttClientDataSourceInfoMagicResourceStorage,
                magicDynamicMqttClientDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClientSubscribeInfoMagicResourceStorage mqttClientSubscribeInfoMagicResourceStorage() {
        return new MqttClientSubscribeInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn("mqttClientDataSourceMagicDynamicRegistry")
    public MqttClientSubscribeMagicDynamicRegistry mqttClientSubscribeMagicDynamicRegistry(
            MqttClientSubscribeInfoMagicResourceStorage mqttClientSubscribeInfoMagicResourceStorage,
            MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        return new MqttClientSubscribeMagicDynamicRegistry(mqttClientSubscribeInfoMagicResourceStorage,
                magicDynamicMqttClientDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClientConsumerService mqttClientConsumerService() {
        return new DefaultMqttClientConsumerService();
    }

    @Override
    public Plugin plugin() {
        return new Plugin("mqttclient", "MagicMqttClient", "magic-mqttclient.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicMqttClientController(configuration));
    }

    @Bean
    public MqttClientModule mqttClientModule(MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource,
                                              @Autowired(required = false) MqttClientTemplate mqttClientTemplate) {
        return new MqttClientModule(magicDynamicMqttClientDataSource, mqttClientTemplate);
    }
}