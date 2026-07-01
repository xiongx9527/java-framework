package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service.VueInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service.VueMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.web.MagicVueController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
@ConditionalOnProperty(prefix = "magic-api.plugin.vue3", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"org.thymeleaf.Thymeleaf"})
@Configuration
@EnableConfigurationProperties(MagicVue3Config.class)
@Import(Vue3ResourceConfiguration.class)
public class MagicAPIVueConfiguration implements MagicPluginConfiguration {
    /**
     * MagicAPI配置属性对象
     *
     * 该字段用于存储MagicAPI的相关配置信息，通过构造函数注入。
     * 使用final修饰确保配置属性在初始化后不可更改，保证配置的稳定性和线程安全性。
     */
    private final MagicAPIProperties magicAPIProperties;

    /**
     * MagicAPIVueConfiguration 构造函数
     *
     * @param magicAPIProperties MagicAPI的配置属性对象，用于初始化Vue配置
     */
    public MagicAPIVueConfiguration(MagicAPIProperties magicAPIProperties) {
        this.magicAPIProperties = magicAPIProperties;
    }

    /**
     * 该方法用于提供当前使用的插件信息
     *
     * @return Plugin 实例，包含插件的名称、描述和文件名
     */
    @Override
    public Plugin plugin() {
        // 创建并返回一个Plugin对象，参数分别为插件的标识符、描述和主文件名
        return new Plugin("vue3", "MagicVue3","magic-vue3.1.0.0.iife.js");
    }

    /**
     * 创建Vue模块的Bean实例
     *
     * @return VueModule Vue模块实例
     */
    @Bean
    public VueModule vueModule() {
        return new VueModule(magicAPIProperties);
    }

    @Bean
    public WebMvcConfigurer vue3StaticResourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler(magicAPIProperties.getWeb() + "/magic-editor/vue3/**")
                        .addResourceLocations("classpath:/magic-editor/vue3/");
            }
        };
    }

    /**
     * 提供一个控制器注册方法，用于在系统中注册一个魔术Vue控制器
     * 这个方法通过返回一个实现了特定接口的注册器对象，使得外部可以在不直接依赖具体实现的情况下进行控制器的注册
     * 主要用途是在框架或系统初始化时，将自定义的控制器注册到系统中，以便框架能够识别并正确路由用户请求
     *
     * @return MagicControllerRegister 实现了ControllerRegister接口的实现类，用于注册控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicVueController(configuration,magicAPIProperties,new VueModule(magicAPIProperties)));
    }


    /**
     * 创建VueInfoMagicResourceStorage的Bean实例
     *
     * 此方法利用Spring框架的@Bean注解来定义一个Bean，
     * 并使用@ConditionalOnMissingBean注解确保当容器中没有该类型Bean时才创建
     * 这样做可以避免Bean的重复创建，保证应用的高效运行
     *
     * @return 新建的VueInfoMagicResourceStorage实例
     */
    @Bean
    @ConditionalOnMissingBean
    public VueInfoMagicResourceStorage vue3InfoMagicResourceStorage() {
        return new VueInfoMagicResourceStorage();
    }


    /**
     * 创建一个VueMagicDynamicRegistry的Bean实例
     * 该方法利用Spring的条件注解来控制Bean的创建时机，只有在容器中没有同类型Bean存在时才会创建
     * 这样做可以避免重复创建Bean实例，提高应用的性能
     *
     * @param vue3InfoMagicResourceStorage 一个用于存储Vue信息魔法资源的存储器
     * @return 返回一个VueMagicDynamicRegistry实例，用于动态注册Vue相关的魔法资源
     */
    @Bean
    @ConditionalOnMissingBean
    public VueMagicDynamicRegistry vue3MagicDynamicRegistry(VueInfoMagicResourceStorage vue3InfoMagicResourceStorage) {
        return new VueMagicDynamicRegistry(vue3InfoMagicResourceStorage);
    }

}
