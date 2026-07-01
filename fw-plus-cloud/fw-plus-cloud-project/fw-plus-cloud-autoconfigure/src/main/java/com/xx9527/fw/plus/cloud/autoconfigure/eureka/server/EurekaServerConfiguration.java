package com.xx9527.fw.plus.cloud.autoconfigure.eureka.server;

import org.springframework.cloud.netflix.eureka.server.EurekaServerMarkerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(EurekaServerMarkerConfiguration.class)
public class EurekaServerConfiguration {
}
