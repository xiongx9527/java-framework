package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service.ConfigMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.web.MagicConfigController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service.ConfigMagicDynamicRegistry;
@ConditionalOnProperty(prefix = "magic-api.plugin.config", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.springframework.cloud.context.refresh.ContextRefresher"})
@Configuration
@Import({MagicAPIConfServerAutoConfiguration.class})
public class MagicAPIConfAutoConfiguration implements MagicPluginConfiguration {

    /**
     * 创建并返回一个Plugin对象，用于配置信息
     *
     * @return Plugin对象，包含插件的配置信息
     */
    @Override
    public Plugin plugin() {
        return new Plugin("config", "MagicConfig","magic-config.1.0.0.iife.js");
    }

    /**
     * 定义控制器注册逻辑，用于注册MagicConfig控制器
     *
     * @return MagicControllerRegister接口的实现，用于注册控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicConfigController(configuration));
    }

    /**
     * 创建并返回一个ConfigMagicResourceStorage对象，用于存储配置资源
     *
     * @return ConfigMagicResourceStorage对象，用于配置资源的存储
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigMagicResourceStorage configMagicResourceStorage() {
        return new ConfigMagicResourceStorage();
    }

    /**
     * 创建并返回一个ConfigMagicDynamicRegistry对象，用于动态注册配置
     *
     * @param configMagicResourceStorage 配置资源存储对象，用于注册过程
     * @return ConfigMagicDynamicRegistry对象，用于动态注册配置
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    @ConditionalOnMissingBean
    public ConfigMagicDynamicRegistry configMagicDynamicRegistry(ConfigMagicResourceStorage configMagicResourceStorage) {
        return new ConfigMagicDynamicRegistry(configMagicResourceStorage);
    }

}
