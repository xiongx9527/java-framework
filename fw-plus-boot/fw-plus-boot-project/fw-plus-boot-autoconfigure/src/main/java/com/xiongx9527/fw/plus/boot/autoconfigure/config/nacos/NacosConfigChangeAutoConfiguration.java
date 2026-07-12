package com.xiongx9527.fw.plus.boot.autoconfigure.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@ConditionalOnClass(ConfigService.class)
@ConditionalOnProperty(prefix = "fw.plus.config.event", name = "nacos-enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class NacosConfigChangeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NacosConfigChangeAdapter nacosConfigChangeAdapter(
            Environment environment,
            ConfigService nacosConfigService,
            ApplicationEventPublisher eventPublisher) {
        return new NacosConfigChangeAdapter(environment, nacosConfigService, eventPublisher);
    }
}
