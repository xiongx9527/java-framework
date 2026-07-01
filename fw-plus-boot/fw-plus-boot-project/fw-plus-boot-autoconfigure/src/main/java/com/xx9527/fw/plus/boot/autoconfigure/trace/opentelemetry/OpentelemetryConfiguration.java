package com.xx9527.fw.plus.boot.autoconfigure.trace.opentelemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "fw.plus.opentelemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"io.opentelemetry.api.OpenTelemetry"})
@Configuration
public class OpentelemetryConfiguration {

    @Bean
    public OpentelemetryTrace opentelemetryTrace() {
        return new OpentelemetryTrace();
    }

}