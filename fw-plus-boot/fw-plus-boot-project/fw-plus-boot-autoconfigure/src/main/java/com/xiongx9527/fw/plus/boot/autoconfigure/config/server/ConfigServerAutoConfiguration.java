package com.xiongx9527.fw.plus.boot.autoconfigure.config.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.cloud.config.server.config.ConfigServerConfiguration")
@Configuration
@Import(ConfigServerConfiguration.class)
public class ConfigServerAutoConfiguration {
}
