package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Servlet（Spring MVC）栈的信封与异常装配。
 *
 * <p>仅在 Servlet Web 应用且存在 MVC 的 {@link ResponseBodyAdvice} 时生效；
 * WebFlux 应用走 {@link ReactiveResponseAutoConfiguration}。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseBodyAdvice.class)
@ConditionalOnProperty(prefix = ResponseProperties.PREFIX, name = "enabled", havingValue = "true")
public class ServletResponseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResponseEnvelopeAdvice responseEnvelopeAdvice(ResponseProperties properties,
                                                         ResponseEnvelopeFactory factory) {
        return new ResponseEnvelopeAdvice(properties, factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(ResponseEnvelopeFactory factory) {
        return new GlobalExceptionHandler(factory);
    }
}
