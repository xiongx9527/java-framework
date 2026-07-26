package com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.plugin.eureka.starter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@ConditionalOnProperty(prefix = "magic-api.plugin.eureka.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.cloud.netflix.eureka.server.EurekaServerMarkerConfiguration")
@Configuration
public class MagicAPIEurekaConfiguration implements MagicPluginConfiguration {

    @Value("${eureka.dashboard.path:/}")
    private String eurekaDashboardPath  = "/";

    @Override
    public Plugin plugin() {
        Plugin plugin = new Plugin("eureka", "MagicEureka", "magic-eureka.1.0.0.iife.js");
        plugin.getAttributes().put("path", eurekaDashboardPath);
        return plugin;
    }
}
