package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一响应信封的公共配置：属性与信封工厂。
 *
 * <p>与 Web 栈无关（Servlet / Reactive 共用同一份字段名与状态码规则）；
 * 具体切面与异常处理分别由 {@link ServletResponseAutoConfiguration} 和
 * {@link ReactiveResponseAutoConfiguration} 按栈装配。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = ResponseProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ResponseProperties.class)
public class ResponseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResponseEnvelopeFactory responseEnvelopeFactory(ResponseProperties properties) {
        return new ResponseEnvelopeFactory(properties);
    }
}
