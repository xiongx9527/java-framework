package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.module.SpringBeanModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service.SpringBeanMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service.SpringBeanMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(value = MagicConfiguration.class,
        name = "com.xiongx9527.fw.plus.boot.starter.magicapi.springbean.SpringBeanPluginMarker")
@ConditionalOnProperty(prefix = "magic-api.plugin.spring-bean", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SpringBeanProperties.class)
public class MagicAPISpringBeanConfiguration implements MagicPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-spring-bean.1.0.0.iife.js";

    @Bean
    @ConditionalOnMissingBean(name = "springBeanJavaSourceAnalyzer")
    public JavaSourceAnalyzer springBeanJavaSourceAnalyzer() {
        return new JavaSourceAnalyzer();
    }

    @Bean
    @ConditionalOnMissingBean(name = "springBeanJavaCompiler")
    public InMemoryJavaEntityCompiler springBeanJavaCompiler() {
        return new InMemoryJavaEntityCompiler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSpringBeanRegistry dynamicSpringBeanRegistry(ConfigurableApplicationContext applicationContext,
                                                               JavaSourceAnalyzer springBeanJavaSourceAnalyzer,
                                                               InMemoryJavaEntityCompiler springBeanJavaCompiler) {
        return new DynamicSpringBeanRegistry(applicationContext, springBeanJavaSourceAnalyzer, springBeanJavaCompiler);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringBeanMagicResourceStorage springBeanMagicResourceStorage(JavaSourceAnalyzer springBeanJavaSourceAnalyzer,
                                                                         DynamicSpringBeanRegistry registry) {
        return new SpringBeanMagicResourceStorage(springBeanJavaSourceAnalyzer, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringBeanMagicDynamicRegistry springBeanMagicDynamicRegistry(SpringBeanMagicResourceStorage storage,
                                                                         DynamicSpringBeanRegistry registry) {
        return new SpringBeanMagicDynamicRegistry(storage, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringBeanModule springBeanModule(DynamicSpringBeanRegistry registry) {
        return new SpringBeanModule(registry);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("spring-bean", "MagicSpringBean", JAVASCRIPT_FILENAME);
    }
}
