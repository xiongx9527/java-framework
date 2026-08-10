package com.xiongx9527.fw.plus.boot.autoconfigure.config;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.apollo.ApolloConfigChangeAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigEventAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.nacos.NacosConfigChangeAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.server.ConfigServerAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Config 模块自动配置聚合入口。
 *
 * <p>通过 {@link Import} 聚合 Config Server、Config Client、Config Event、
 * Apollo 和 Nacos 五个子配置，使 {@code spring.factories} 中 Config 模块
 * 从 5 行收敛为 1 行。</p>
 *
 * <p>各子配置自带 {@code @ConditionalOnClass} / {@code @ConditionalOnProperty}
 * 条件，按 classpath 和配置自动激活，互不干扰。</p>
 */
@Configuration(proxyBeanMethods = false)
@Import({
		ConfigServerAutoConfiguration.class,
		ConfigClientAutoConfiguration.class,
		ConfigEventAutoConfiguration.class,
		ApolloConfigChangeAutoConfiguration.class,
		NacosConfigChangeAutoConfiguration.class
})
public class ConfigConfiguration {
}
