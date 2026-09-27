package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.MagicAPIDynamicDataSourceAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxConditionalConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * MagicAPI 模块自动配置聚合入口。
 *
 * <p>环境无关的统一入口，通过 {@link Import} 聚合 SERVLET 模式（{@link MagicAPIWebMvcAutoConfiguration}）、
 * REACTIVE 模式（{@link MagicAPIWebFluxAutoConfiguration}）以及动态数据源支持
 * （{@link MagicAPIDynamicDataSourceAutoConfiguration}）三个子配置，
 * 使 {@code spring.factories} 中 MagicAPI 模块从 3 行收敛为 1 行。</p>
 *
 * <p>子配置各自带 {@code @ConditionalOnWebApplication(type = SERVLET / REACTIVE)} 条件，
 * 按应用类型自动激活对应的子配置，互不干扰。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
		MagicAPIWebMvcAutoConfiguration.class,
		MagicAPIWebFluxConditionalConfiguration.class,
		MagicAPIDynamicDataSourceAutoConfiguration.class
})
public class MagicAPIBaseAutoConfiguration {
}
