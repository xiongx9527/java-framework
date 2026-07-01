package com.xiongx9527.fw.plus.boot.autoconfigure.web.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webflux.filter.WebFluxCommonFilter;
import com.xiongx9527.fw.plus.boot.common.CommonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@Configuration
//@ConditionalOnExpression("#{T(com.xiongx9527.fw.plus.common.util.SpringUtils).isWebFlux()}")
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.REACTIVE
)
@Import({WebFluxCommonFilter.class})
public class WebFluxAutoConfiguration extends CommonAutoConfiguration {

    /**
     * Web属性配置对象，用于存储和提供Web相关的配置信息
     */
    private final WebProperties webProperties;

    /**
     * WebMvc自动配置类的构造函数
     *
     * @param webProperties Web属性配置对象，包含Web应用的相关配置信息
     */
    public WebFluxAutoConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     * 创建并返回一个WebFlux请求监控处理器实例
     *
     * @return WebFluxRequestMonitorHandler类型的请求监控处理器实例
     *         该处理器用于监控和处理WebFlux请求的相关操作
     */
    @Bean
    public WebFluxRequestMonitorHandler webFluxRequestMonitorHandler() {
        return new WebFluxRequestMonitorHandler(webProperties);
    }

}
