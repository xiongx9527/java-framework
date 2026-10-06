package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.Langchain4jModuleConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux.MagicAPIWebFluxConditionalConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * AI 层 magicapi 插件装配入口，环境无关（对齐 Boot 层 {@code MagicAPIBaseAutoConfiguration} 风格）：
 * {@code MagicApiPluginAutoConfiguration} 与 {@code Langchain4jModuleConfiguration} 只提供
 * web 栈无关的功能 bean（数据源、注册表、存储、模块），SERVLET 由 fork 官方 starter 消费
 * {@code controllerRegister()} 注册端点，REACTIVE 由 {@link MagicAPIWebFluxConditionalConfiguration}
 * 桥接的插件 Registrar 注册端点。
 *
 * <p>排序说明：reactive 下插件配置带 {@code @ConditionalOnBean(MagicResourceService.class)}，
 * 该 bean 由 Boot 层 reactive 引擎（{@code MagicAPIWebFluxAutoConfiguration}）提供，必须先于本配置
 * 评估；servlet 下则须排在 fork 官方 starter 之后。</p>
 */
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@AutoConfigureAfter(name = {
        // 插件配置带 @ConditionalOnBean(MagicResourceService.class)：servlet 下须在 magic-api starter
        // 之后评估；reactive 下须在 Boot 层 reactive 引擎之后评估，否则该 bean 尚未注册，插件不生效。
        "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration",
        "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration",
        // KnowledgeConfiguration 带 @ConditionalOnBean(SqlSessionFactory.class)：必须排在提供
        // sqlSessionFactory 的自动配置之后评估。@ConditionalOnBean 只查「已注册的 bean 定义」，
        // 顺序反了就永远 did not find any beans——而且不抛异常，只表现为 knowledge 插件静默不注册
        // （控制台无入口，条件评估报告里一行否定记录而已，排查成本极高）。
        // 两个都列上：fw-plus 的 MpAutoConfiguration 自身已 @AutoConfigureAfter 了 baomidou 的
        // MybatisPlusAutoConfiguration（sqlSessionFactory 由后者提供），列全是为了覆盖
        // 未引入 fw-plus mp 自动配置、直接用 baomidou starter 的变体。
        "com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpAutoConfiguration",
        "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@Import({MagicApiPluginAutoConfiguration.class, Langchain4jModuleConfiguration.class,
        MagicAPIWebFluxConditionalConfiguration.class})
public class MagicAPIBaseAutoConfiguration {
}
