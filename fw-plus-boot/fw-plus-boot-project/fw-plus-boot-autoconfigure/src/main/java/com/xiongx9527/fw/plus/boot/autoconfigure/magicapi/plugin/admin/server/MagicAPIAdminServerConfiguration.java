package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

/**
 * MagicAPI管理服务器插件配置类
 * 当启用管理服务器插件（magic-api.plugin.admin.server.enabled=true）且Spring Boot Admin相关类存在时自动装配
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.admin.server", name = "enabled", havingValue = "true", matchIfMissing = true)
//@ConditionalOnBean(type = {"de.codecentric.boot.admin.server.ui.web.UiController"})
@ConditionalOnClass(name = "de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration")
@Configuration
public class MagicAPIAdminServerConfiguration implements MagicPluginConfiguration {
	private static final String JAVASCRIPT_FILENAME = "magic-admin.1.0.0.iife.js";

    /**
     * Spring Boot Admin的上下文路径，从配置文件中读取
     * 默认为空字符串，可通过spring.boot.admin.context-path属性配置
     */
    @Value("${spring.boot.admin.context-path:}")
    private String springbootAdminContextPath = "";

    /**
     * 注册管理服务器插件标识。
     *
     * <p>始终上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物是否落在 classpath
     * 上改变上报内容——产物在不在 classpath 是使用方的依赖配置问题，编码进注册逻辑只会让
     * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
     *
     * @return 管理服务器插件对象，名称为"admin"，并包含配置的上下文路径
     */
    @Override
    public Plugin plugin() {
		Plugin plugin = new Plugin("admin", "MagicAdmin", JAVASCRIPT_FILENAME);
        plugin.getAttributes().put("path", springbootAdminContextPath);
        return plugin;
    }
}
