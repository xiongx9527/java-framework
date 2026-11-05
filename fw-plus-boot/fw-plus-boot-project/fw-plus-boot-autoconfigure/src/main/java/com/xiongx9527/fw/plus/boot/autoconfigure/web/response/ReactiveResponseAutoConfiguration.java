package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

/**
 * Reactive（WebFlux）栈的信封与异常装配。
 *
 * <p>仅在 Reactive Web 应用且存在 WebFlux 的 {@link ResponseBodyAdvice} 时生效；
 * Servlet 应用走 {@link ServletResponseAutoConfiguration}。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(WebFilter.class)
@ConditionalOnProperty(prefix = ResponseProperties.PREFIX, name = "enabled", havingValue = "true")
public class ReactiveResponseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveResponseEnvelopeFilter reactiveResponseEnvelopeFilter(ResponseProperties properties,
                                                                         ResponseEnvelopeFactory factory,
                                                                         ObjectProvider<ObjectMapper> objectMapper) {
        return new ReactiveResponseEnvelopeFilter(properties, factory,
                objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveGlobalExceptionHandler reactiveGlobalExceptionHandler(ResponseEnvelopeFactory factory) {
        return new ReactiveGlobalExceptionHandler(factory);
    }
}
