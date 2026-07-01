package com.xiongx9527.fw.plus.boot.autoconfigure.config.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@ConditionalOnProperty(prefix = "fw.plus.config.client", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@Configuration
@EnableConfigurationProperties({ConfigClientProperties.class,
        com.xiongx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties.class})
public class ConfigClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ConfigServicePropertySourceLocator.class)
    @ConditionalOnClass(ContextRefresher.class)
    @ConditionalOnProperty(prefix = "fw.plus.config.client", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
    public ConfigRefresher configRefresher(
            com.xiongx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties clientProperties,
            ConfigServicePropertySourceLocator locator,
            ConfigurableEnvironment environment,
            ContextRefresher contextRefresher,
            ApplicationEventPublisher eventPublisher) {
        return new ConfigRefresher(clientProperties, locator, environment, contextRefresher, eventPublisher);
    }
}
