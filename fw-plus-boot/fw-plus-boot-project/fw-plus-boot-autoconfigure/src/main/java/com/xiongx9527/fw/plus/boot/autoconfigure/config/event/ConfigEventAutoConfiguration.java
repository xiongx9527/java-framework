package com.xiongx9527.fw.plus.boot.autoconfigure.config.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public static ConfigChangeEventListener configChangeEventListener() {
        return new ConfigChangeEventListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingLevelRefresher loggingLevelRefresher() {
        return new LoggingLevelRefresher();
    }
}
