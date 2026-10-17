
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer.DefaultMqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer.MqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.module.MqttClientModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.web.MagicMqttClientController;
import org.dromara.mica.mqtt.spring.client.MqttClientTemplate;
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
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.mqttclient", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.mica.mqtt.spring.client.MqttClientTemplate"})
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
    public MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage() {
        return new MqttClientDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClientDataSourceMagicDynamicRegistry mqttClientDataSourceMagicDynamicRegistry(
            MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage,
            MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource,
            MqttClientSubscribeInfoMagicResourceStorage mqttClientSubscribeInfoMagicResourceStorage) {
        return new MqttClientDataSourceMagicDynamicRegistry(mqttClientDataSourceInfoMagicResourceStorage,
                magicDynamicMqttClientDataSource, mqttClientSubscribeInfoMagicResourceStorage);
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
