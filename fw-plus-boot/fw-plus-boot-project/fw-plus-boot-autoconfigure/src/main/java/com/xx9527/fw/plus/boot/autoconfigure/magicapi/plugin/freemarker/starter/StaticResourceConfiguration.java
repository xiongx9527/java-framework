package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.filter.StaticResourceFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//@ConditionalOnClass(name = "javax.servlet.http.HttpServletRequest")
@Configuration
public class StaticResourceConfiguration {

    @Value("${magic.api.plugin.freemarker.static-file-excludes:}")
    private String[] staticFileExcludes = new String[]{};

    /**
     * 创建并配置静态资源过滤器Bean
     *
     * @return StaticResourceFilter 静态资源过滤器实例
     */
    @Bean
    public StaticResourceFilter staticFileFilter() {
        return new StaticResourceFilter();
    }


    /**
     * 创建并配置静态文件过滤器注册Bean。
     * 该方法将创建一个FilterRegistrationBean实例，用于注册静态文件过滤器，并配置需要排除的静态文件类型。
     *
     * @param staticFileFilter 静态文件过滤器实例
     * @return 配置好的FilterRegistrationBean实例
     */
    @Bean
    public FilterRegistrationBean<StaticResourceFilter> staticResourceFilterRegistration(StaticResourceFilter staticFileFilter) {
        // 创建过滤器注册Bean
        FilterRegistrationBean<StaticResourceFilter> registration = new FilterRegistrationBean<>(staticFileFilter);
        // 定义默认需要排除的静态文件类型
        List<String> defaultStaticFileExcludes = Arrays.asList("*.js","*.css","*.html","*.xml","*.ttf","*.woff","*.woff2","*.eot","*.otf");
        // 获取自定义的静态文件排除列表
        List<String> staticFileExcludes = Arrays.asList(this.staticFileExcludes);
        // 合并默认和自定义的排除列表
        List<String> allStaticFileExcludes = new ArrayList<>();
        allStaticFileExcludes.addAll(defaultStaticFileExcludes);
        allStaticFileExcludes.addAll(staticFileExcludes);
        // 将合并后的排除列表添加到过滤器注册中
        registration.addUrlPatterns(allStaticFileExcludes.stream().toArray(String[]::new));
        return registration;
    }
}
