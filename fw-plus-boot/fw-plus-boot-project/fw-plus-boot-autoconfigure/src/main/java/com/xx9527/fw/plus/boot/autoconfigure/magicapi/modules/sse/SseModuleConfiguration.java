package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(SseHttpProperties.class)
@Configuration
@ConditionalOnClass(name="com.xx9527.fw.plus.dify.DifyClientBuilder")
public class SseModuleConfiguration {
    
    
    /**
     * 创建SseHttpModule的Bean实例
     * 当容器中没有SseHttpModule类型的Bean时，自动创建并注册该Bean
     *
     * @param sseHttpProperties SSE HTTP配置属性，包含超时时间等配置
     * @return SseHttpModule实例，用于处理SSE HTTP请求
     */
    @Bean
    @ConditionalOnMissingBean
    public SseHttpModule sseHttpModule(SseHttpProperties sseHttpProperties) {
        return new SseHttpModule(sseHttpProperties);
    }
    
    
}
