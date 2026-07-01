package com.xx9527.fw.plus.boot.autoconfigure.magicapi.filter.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

@ConditionalOnProperty(prefix = "magic.api.resource", name = "read-only", havingValue = "false", matchIfMissing = true)
@Configuration
public class AuthConfiguration {

    /**
     * MagicAPI属性配置类实例
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于初始化AuthConfiguration实例
     *
     * @param properties MagicAPI属性配置对象，用于配置认证相关参数
     */
    public AuthConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }


    /**
     * 创建并配置认证过滤器Bean
     *
     * @return AuthFilter 认证过滤器实例
     */
    @Bean
    public AuthFilter authFilter(MagicAPIProperties properties) {
        return new AuthFilter(properties);
    }

    /**
     * 注册自定义认证过滤器
     *
     * @param authFilter 认证过滤器实例
     * @return 返回配置好的过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter authFilter) {
        String webBasePath = properties != null && properties.getWeb() != null ? properties.getWeb() : "";
        // 确保webBasePath以/开头
        if (!webBasePath.startsWith("/") && !webBasePath.isEmpty()) {
            webBasePath = "/" + webBasePath;
        }
        // 避免以/结尾导致双斜杠
        if (webBasePath.endsWith("/")) {
            webBasePath = webBasePath.substring(0, webBasePath.length() - 1);
        }        // 创建过滤器注册Bean并设置URL匹配模式
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>(authFilter);
        registration.addUrlPatterns(webBasePath + "/*");
        return registration;
    }

}
