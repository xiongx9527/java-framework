package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

/**
 * AI 层 magicapi 插件 WebFlux 子配置：在 Boot 层 reactive 引擎就绪的前提下，
 * 把 AI 侧插件控制器（langchain4j、mcp）注册进响应式 {@code RequestMappingHandlerMapping}。
 *
 * <p>插件功能 bean（数据源、注册表、存储、模块）由 {@code MagicApiPluginAutoConfiguration}
 * 环境无关提供，本类只负责 web 端点注册。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBean(MagicAPIWebFluxMapping.class)
public class MagicAPIWebFluxPluginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicAPIWebFluxPluginControllerRegistrar magicAPIWebFluxPluginControllerRegistrar(
            MagicAPIWebFluxMapping mapping,
            MagicConfiguration configuration,
            ApplicationContext applicationContext) {
        return new MagicAPIWebFluxPluginControllerRegistrar(mapping, configuration, applicationContext);
    }
}
