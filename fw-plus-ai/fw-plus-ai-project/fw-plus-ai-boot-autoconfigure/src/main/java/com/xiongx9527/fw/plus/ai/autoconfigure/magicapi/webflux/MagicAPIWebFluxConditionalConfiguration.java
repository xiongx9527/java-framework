package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.webflux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * AI 层 magicapi 插件 WebFlux 子配置的条件化入口（对齐 Boot 层同名模式）。
 *
 * <p>本类经 {@code MagicAPIBaseAutoConfiguration} 的 {@code @Import} 无条件加载，自身不引用
 * 任何 webflux 类型（可被安全加载）；由它按字符串条件再导入真正的子配置——classpath 无
 * spring-webflux 时 {@code Class} 形式引用的 webflux 类型会加载即失败，{@code @ConditionalOnClass}
 * 来不及生效。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.reactive.config.WebFluxConfigurer")
@Import(MagicAPIWebFluxPluginAutoConfiguration.class)
public class MagicAPIWebFluxConditionalConfiguration {
}
