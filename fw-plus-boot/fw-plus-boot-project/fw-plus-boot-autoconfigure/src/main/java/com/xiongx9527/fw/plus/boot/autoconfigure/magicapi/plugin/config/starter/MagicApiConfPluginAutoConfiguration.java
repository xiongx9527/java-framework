package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * magic-api config 插件的自动配置入口。
 *
 * <p>插件本体 {@link MagicAPIConfAutoConfiguration} 带 {@code @ConditionalOnBean(ContextRefresher)}，
 * 而该 bean 由 spring-cloud-context 的 {@code RefreshAutoConfiguration} 注册。
 * 原来插件只被 {@code MagicApiPluginAutoConfiguration} 以 @Import 聚合，条件评估时机
 * 早于 cloud 注册，导致插件永不激活；这里作为独立自动配置并声明顺序。</p>
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration")
@Import(MagicAPIConfAutoConfiguration.class)
public class MagicApiConfPluginAutoConfiguration {
}
