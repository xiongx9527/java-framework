package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@ConditionalOnProperty(prefix = "magic-api.plugin.admin.server", name = "enabled", havingValue = "true", matchIfMissing = true)
//@ConditionalOnBean(type = {"de.codecentric.boot.admin.server.ui.web.UiController"})
@ConditionalOnClass(name = "de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration")
@Configuration
public class MagicAPIAdminServerConfiguration implements MagicPluginConfiguration {

    @Value("${spring.boot.admin.context-path:}")
    private String springbootAdminContextPath = "";

    @Override
    public Plugin plugin() {
        return new Plugin("admin",springbootAdminContextPath);
    }
}
