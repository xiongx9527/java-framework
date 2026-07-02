package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.properties.DruidStatProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * @author lihengming [89921218@qq.com]
 */
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "magic-api.plugin.druid.stat-view-servlet.enabled", havingValue = "true")
public class DruidStatViewServletConfiguration {

    /**
     * 默认允许访问的IP地址常量
     * 该常量定义了默认情况下被允许访问的IP地址，设置为本地回环地址127.0.0.1
     */
    private static final String DEFAULT_ALLOW_IP = "127.0.0.1";


    /**
     * 创建Druid监控页面的Servlet注册Bean
     *
     * @param properties Druid统计属性配置对象，包含监控页面的相关配置信息
     * @return 配置好的ServletRegistrationBean对象，用于注册Druid监控页面Servlet
     */
    @Bean
    public ServletRegistrationBean statViewServletRegistrationBean(DruidStatProperties properties) {
        // 获取Druid监控页面Servlet的配置信息
        DruidStatProperties.StatViewServlet config = properties.getStatViewServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean();
        registrationBean.setServlet(new StatViewServlet());

        // 设置URL映射路径，如果未配置则使用默认路径"/druid/*"
        registrationBean.addUrlMappings(config.getUrlPattern() != null ? config.getUrlPattern() : "/druid/*");

        // 设置允许访问的IP地址列表
        if (config.getAllow() != null) {
            registrationBean.addInitParameter("allow", config.getAllow());
        } else {
            registrationBean.addInitParameter("allow", DEFAULT_ALLOW_IP);
        }

        // 设置拒绝访问的IP地址列表
        if (config.getDeny() != null) {
            registrationBean.addInitParameter("deny", config.getDeny());
        }

        // 设置登录用户名
        if (config.getLoginUsername() != null) {
            registrationBean.addInitParameter("loginUsername", config.getLoginUsername());
        }

        // 设置登录密码
        if (config.getLoginPassword() != null) {
            registrationBean.addInitParameter("loginPassword", config.getLoginPassword());
        }

        // 设置是否允许重置监控数据
        if (config.getResetEnable() != null) {
            registrationBean.addInitParameter("resetEnable", config.getResetEnable());
        }

        return registrationBean;
    }


}
