package com.xx9527.fw.plus.boot.autoconfigure.admin.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.admin.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration")
@Configuration
@Import(AdminServerConfiguration.class)
public class AdminServerAutoConfiguration {

}
