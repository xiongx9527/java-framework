package com.xiongx9527.fw.plus.cloud.autoconfigure.eureka.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.cloud.eureka.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.cloud.netflix.eureka.server.EurekaServerMarkerConfiguration")
@Configuration
@Import(EurekaServerConfiguration.class)
public class EurekaServerAutoConfiguration {
}
