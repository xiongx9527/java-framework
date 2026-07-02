package com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "fw.plus.opentelemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"io.opentelemetry.api.OpenTelemetry"})
@Configuration
public class OpentelemetryConfiguration {

    /**
     * 创建并配置OpenTelemetry跟踪实例
     *
     * 该方法用于创建一个OpentelemetryTrace bean，用于应用程序的分布式跟踪功能。
     * 通过Spring的依赖注入机制，该bean可以在应用程序中被其他组件注入和使用，
     * 以实现请求链路追踪、性能监控等功能。
     *
     * @return OpentelemetryTrace实例，用于处理OpenTelemetry跟踪相关操作
     */
    @Bean
    public OpentelemetryTrace opentelemetryTrace() {
        return new OpentelemetryTrace();
    }


}
