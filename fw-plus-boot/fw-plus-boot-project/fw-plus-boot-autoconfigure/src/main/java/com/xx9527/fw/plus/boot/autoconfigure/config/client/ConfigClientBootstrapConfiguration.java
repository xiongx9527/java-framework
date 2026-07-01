package com.xx9527.fw.plus.boot.autoconfigure.config.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@ConditionalOnProperty(prefix = "fw.plus.config.client", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@Configuration
@EnableConfigurationProperties({ConfigClientProperties.class,
        com.xx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties.class})
public class ConfigClientBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CachingConfigServicePropertySourceLocator configServicePropertySourceLocator(
            ConfigClientProperties standardProperties,
            com.xx9527.fw.plus.boot.autoconfigure.config.client.ConfigClientProperties clientProperties,
            Environment environment) {
        return new CachingConfigServicePropertySourceLocator(standardProperties, clientProperties, environment);
    }
}
