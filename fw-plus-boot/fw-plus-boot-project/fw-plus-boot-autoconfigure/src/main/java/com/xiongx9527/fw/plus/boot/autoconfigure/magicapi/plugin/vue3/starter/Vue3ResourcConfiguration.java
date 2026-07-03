package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.filter.Vue3ResourcFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class Vue3ResourcConfiguration {
    /**
     * 创建Vue3资源过滤器Bean
     *
     * @param vueModule Vue模块实例
     * @return Vue3资源过滤器实例
     */
    @Bean
    public Vue3ResourcFilter vue3ResourcFilter(VueModule vueModule) {
        return new Vue3ResourcFilter(vueModule);
    }

    /**
     * 创建Vue3资源过滤器注册Bean
     * 用于注册Vue3资源过滤器并设置URL匹配模式
     *
     * @param vue3ResourcFilter Vue3资源过滤器实例
     * @return 过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<Vue3ResourcFilter> vue3ResourcFilterRegistration(Vue3ResourcFilter vue3ResourcFilter) {
        // 创建过滤器注册Bean
        FilterRegistrationBean<Vue3ResourcFilter> registration = new FilterRegistrationBean<>(vue3ResourcFilter);
        registration.setFilter(vue3ResourcFilter);
        registration.setUrlPatterns(Arrays.asList(new String[]{"*.vue"}));
        return registration;
    }

}
