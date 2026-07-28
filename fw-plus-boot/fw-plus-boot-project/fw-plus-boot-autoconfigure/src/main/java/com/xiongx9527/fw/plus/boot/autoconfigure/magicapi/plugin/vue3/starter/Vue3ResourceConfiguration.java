package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.filter.Vue3ResourceFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.util.Collections;

@Configuration
public class Vue3ResourceConfiguration {

    @Bean
    public Vue3ResourceFilter vue3ResourceFilter(MagicAPIProperties magicAPIProperties,VueModule vueModule) {
        return new Vue3ResourceFilter(magicAPIProperties,vueModule);
    }

    @Bean
    public FilterRegistrationBean<Vue3ResourceFilter> vue3ResourceFilterRegistration(
            Vue3ResourceFilter vue3ResourceFilter,
            MagicAPIProperties magicAPIProperties) {
        FilterRegistrationBean<Vue3ResourceFilter> registration = new FilterRegistrationBean<>(vue3ResourceFilter);
        registration.setUrlPatterns(Collections.singletonList(
                magicAPIProperties.getWeb() + "/vue3/component/*"));
        return registration;
    }
}
