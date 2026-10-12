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
	private static final String JAVASCRIPT_FILENAME = "magic-eureka.1.0.0.iife.js";

    @Value("${eureka.dashboard.path:/}")
    private String eurekaDashboardPath  = "/";

    /**
     * 创建 Eureka 插件。
     *
     * <p>始终上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物是否落在 classpath
     * 上改变上报内容——产物在不在 classpath 是使用方的依赖配置问题，编码进注册逻辑只会让
     * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
     */
    @Override
    public Plugin plugin() {
		Plugin plugin = new Plugin("eureka", "MagicEureka", JAVASCRIPT_FILENAME);
        plugin.getAttributes().put("path", eurekaDashboardPath);
        return plugin;
    }
}
