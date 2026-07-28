package com.xiongx9527.fw.plus.boot.autoconfigure.config.apollo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@ConditionalOnClass(com.ctrip.framework.apollo.ConfigService.class)
@ConditionalOnProperty(prefix = "fw.plus.config.event", name = "apollo-enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class ApolloConfigChangeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApolloConfigChangeAdapter apolloConfigChangeAdapter(
            Environment environment,
            ApplicationEventPublisher eventPublisher) {
        return new ApolloConfigChangeAdapter(environment, eventPublisher);
    }
}
