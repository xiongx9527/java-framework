package com.xiongx9527.fw.plus.boot.autoconfigure.config.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@Configuration
@Import(org.springframework.cloud.config.server.config.ConfigServerConfiguration.class)
public class ConfigServerConfiguration {
}
