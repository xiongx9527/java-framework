package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat;

import com.alibaba.druid.support.http.WebStatFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * @author lihengming [89921218@qq.com]
 */
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "magic-api.plugin.druid.stat-view-servlet", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DruidWebStatFilterConfiguration {

    /**
     * 创建并配置Druid监控的Web统计过滤器注册Bean
     *
     * @param properties Druid统计属性配置对象，包含Web统计过滤器的相关配置
     * @return 配置完成的过滤器注册Bean，用于注册Druid的WebStatFilter
     */
    @Bean
    public FilterRegistrationBean webStatFilterRegistrationBean(DruidStatProperties properties) {
        // 获取Web统计过滤器配置
        DruidStatProperties.WebStatFilter config = properties.getWebStatFilter();
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        WebStatFilter filter = new WebStatFilter();
        registrationBean.setFilter(filter);

        // 设置URL匹配模式，默认为拦截所有请求
        registrationBean.addUrlPatterns(config.getUrlPattern() != null ? config.getUrlPattern() : "/*");

        // 设置需要排除的文件类型和路径，默认排除静态资源和Druid监控页面
        registrationBean.addInitParameter("exclusions", config.getExclusions() != null ? config.getExclusions() : "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");

        // 根据配置设置会话统计相关参数
        if (config.getSessionStatEnable() != null) {
            registrationBean.addInitParameter("sessionStatEnable", config.getSessionStatEnable());
        }
        // 配置会话统计最大数量参数
        if (config.getSessionStatMaxCount() != null) {
            registrationBean.addInitParameter("sessionStatMaxCount", config.getSessionStatMaxCount());
        }
        // 配置主体会话名称参数
        if (config.getPrincipalSessionName() != null) {
            registrationBean.addInitParameter("principalSessionName", config.getPrincipalSessionName());
        }
        // 配置主体Cookie名称参数
        if (config.getPrincipalCookieName() != null) {
            registrationBean.addInitParameter("principalCookieName", config.getPrincipalCookieName());
        }
        // 配置性能分析启用参数
        if (config.getProfileEnable() != null) {
            registrationBean.addInitParameter("profileEnable", config.getProfileEnable());
        }

        return registrationBean;
    }


}
