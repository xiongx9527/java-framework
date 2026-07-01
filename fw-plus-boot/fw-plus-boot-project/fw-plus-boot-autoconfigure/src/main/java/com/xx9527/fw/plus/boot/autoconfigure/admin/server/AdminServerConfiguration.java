package com.xx9527.fw.plus.boot.autoconfigure.admin.server;

import de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@Configuration
@Import(AdminServerMarkerConfiguration.class)
public class AdminServerConfiguration {
}
