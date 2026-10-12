package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * magic-api config 插件的自动配置入口。
 *
 * <p>插件本体 {@link MagicAPIConfAutoConfiguration} 带 {@code @ConditionalOnBean(ContextRefresher)}，
 * 而该 bean 由 spring-cloud-context 的 {@code RefreshAutoConfiguration} 注册。
 * 原来插件只被 {@code MagicApiPluginAutoConfiguration} 以 @Import 聚合，条件评估时机
 * 早于 cloud 注册，导致插件永不激活；这里作为独立自动配置并声明顺序。</p>
 *
 * <p>magic-api core 缺失时本类必须整体跳过：注册阶段生成 bean 名会急切解析
 * {@code @Import(MagicAPIConfAutoConfiguration.class)} 的 Class 值，而插件本体实现了
 * magic-api core 的 {@code MagicPluginConfiguration}，类加载将因接口缺失失败
 * （NoClassDefFoundError），导致无 magic-api core 的应用上下文启动失败。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.ssssssss.magicapi.core.config.MagicPluginConfiguration")
@AutoConfigureAfter(name = "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration")
@Import(MagicAPIConfAutoConfiguration.class)
public class MagicApiConfPluginAutoConfiguration {
}
