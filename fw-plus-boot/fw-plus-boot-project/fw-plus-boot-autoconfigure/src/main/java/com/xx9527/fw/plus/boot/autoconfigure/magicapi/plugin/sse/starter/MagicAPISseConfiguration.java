package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.BaseDynamicSseHandler;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.SimpleDynamicSseHandler;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.module.SseModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.web.MagicSseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * @author zzy
 */
@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.sse", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(MagicAPISseConfig.class)

@Import(MagicAPISseHandlerConfiguration.class)
public class MagicAPISseConfiguration implements MagicPluginConfiguration , BaseService {

    /**
     * MagicAPI SSE配置类的属性对象
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于创建MagicAPISseConfiguration实例
     *
     * @param properties MagicAPI的配置属性对象，包含SSE相关配置信息
     */
    public MagicAPISseConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建并配置一个 SseDataSourceInfoMagicResourceStorage 实例，用于存储 SSE 数据源信息。
     *
     * @return SseDataSourceInfoMagicResourceStorage 实例
     */
    @Bean
    public SseDataSourceInfoMagicResourceStorage sseDataSourceInfoMagicResourceStorage() {
        return new SseDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建并配置一个 SseSubscribeInfoMagicResourceStorage 实例，用于存储 SSE 订阅信息。
     *
     * @return SseSubscribeInfoMagicResourceStorage 实例
     */
    @Bean
    public SseSubscribeInfoMagicResourceStorage sseSubscribeInfoMagicResourceStorage() {
        return new SseSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建并配置 BaseDynamicSseHandler 实例，用于处理动态 SSE 请求。
     * 该 Bean 依赖于 sseDataSourceInfoMagicResourceStorage 和 wsSubscribeInfoMagicResourceStorage，
     * 并且仅在 StringRedisTemplate 不存在时才会创建。
     *
     * @param magicDataSourceStorage 数据源存储实例
     * @param sseSubscribeInfoMagicResourceStorage 订阅信息存储实例
     * @return BaseDynamicSseHandler 实例
     */
    @Bean
    @DependsOn(value = {"sseDataSourceInfoMagicResourceStorage", "sseSubscribeInfoMagicResourceStorage"})
    @ConditionalOnMissingBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
    public BaseDynamicSseHandler simpleDynamicSseHandler(SseDataSourceInfoMagicResourceStorage magicDataSourceStorage
            , SseSubscribeInfoMagicResourceStorage sseSubscribeInfoMagicResourceStorage) {
        return new SimpleDynamicSseHandler(magicDataSourceStorage, sseSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建并配置 SseDataSourceMagicDynamicRegistry 实例，用于注册 SSE 数据源的动态资源。
     * 该 Bean 依赖于 sseDataSourceInfoMagicResourceStorage。
     *
     * @param sseDataSourceInfoMagicResourceStorage 数据源信息存储实例
     * @return SseDataSourceMagicDynamicRegistry 实例
     */
    @Bean
    @DependsOn("sseDataSourceInfoMagicResourceStorage")
    public SseDataSourceMagicDynamicRegistry sseDataSourceMagicDynamicRegistry(
            SseDataSourceInfoMagicResourceStorage sseDataSourceInfoMagicResourceStorage) {
        return new SseDataSourceMagicDynamicRegistry(sseDataSourceInfoMagicResourceStorage);
    }

    /**
     * 创建并配置 SseSubscribeMagicDynamicRegistry 实例，用于注册 SSE 订阅的动态资源。
     * 该 Bean 依赖于 sseDataSourceMagicDynamicRegistry。
     *
     * @param sseSubscribeInfoMagicResourceStorage 订阅信息存储实例
     * @return SseSubscribeMagicDynamicRegistry 实例
     */
    @Bean
    @DependsOn("sseDataSourceMagicDynamicRegistry")
    public SseSubscribeMagicDynamicRegistry sseSubscribeMagicDynamicRegistry(
            SseSubscribeInfoMagicResourceStorage sseSubscribeInfoMagicResourceStorage) {
        return new SseSubscribeMagicDynamicRegistry(sseSubscribeInfoMagicResourceStorage);
    }

    /**
     * 返回当前插件的基本信息。
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和资源路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("sse", "MagicSse", "magic-sse.1.0.0.iife.js");
    }

    /**
     * 提供控制器注册逻辑，将 MagicSseController 注册到系统中。
     *
     * @return MagicControllerRegister 控制器注册器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicSseController(configuration));
    }

    /**
     * 创建并配置 SseModule 实例，用于初始化 SSE 模块。
     *
     * @param baseDynamicSseHandler SSE 处理器实例
     * @return SseModule 模块实例
     */
    @Bean
    public SseModule ssModule(BaseDynamicSseHandler baseDynamicSseHandler) {
        return new SseModule(baseDynamicSseHandler, getDefaultKey());
    }

}
