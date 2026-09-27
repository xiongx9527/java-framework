package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * WebFlux 子配置的条件化入口。
 *
 * <p>{@code spring.factories} 中的自动配置类名会先于条件评估被加载，因此不能把
 * {@link MagicAPIWebFluxAutoConfiguration}（implements WebFluxConfigurer，Class 形式引用）
 * 或 {@link MagicAPIWebFluxImportFilter} 直接写进 factories——classpath 无 spring-webflux 时
 * 加载即失败，而 {@code @ConditionalOnClass} 来不及生效。</p>
 *
 * <p>本类自身不引用任何 webflux 类型，可被安全加载；由它按字符串条件再导入真正的子配置。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.reactive.config.WebFluxConfigurer")
@Import({ MagicAPIWebFluxAutoConfiguration.class, MagicAPIWebFluxImportFilter.class })
public class MagicAPIWebFluxConditionalConfiguration {
}
