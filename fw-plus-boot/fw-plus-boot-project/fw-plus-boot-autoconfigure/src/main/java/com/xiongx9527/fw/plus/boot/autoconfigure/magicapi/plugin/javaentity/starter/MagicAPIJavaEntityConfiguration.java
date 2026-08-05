package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.module.EntityModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(value = MagicConfiguration.class,
        name = "com.xiongx9527.fw.plus.boot.starter.magicapi.javaentity.JavaEntityPluginMarker")
@ConditionalOnProperty(prefix = "magic-api.plugin.java-entity", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MagicAPIJavaEntityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JavaSourceAnalyzer javaEntitySourceAnalyzer() {
        return new JavaSourceAnalyzer();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryJavaEntityCompiler inMemoryJavaEntityCompiler() {
        return new InMemoryJavaEntityCompiler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicEntityRegistry dynamicEntityRegistry(JavaSourceAnalyzer analyzer,
                                                       InMemoryJavaEntityCompiler compiler) {
        return new DynamicEntityRegistry(analyzer, compiler);
    }

    @Bean
    @ConditionalOnMissingBean
    public JavaEntityMagicResourceStorage javaEntityMagicResourceStorage(JavaSourceAnalyzer analyzer) {
        return new JavaEntityMagicResourceStorage(analyzer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JavaEntityMagicDynamicRegistry javaEntityMagicDynamicRegistry(
            JavaEntityMagicResourceStorage storage, DynamicEntityRegistry registry) {
        return new JavaEntityMagicDynamicRegistry(storage, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityModule entityModule(DynamicEntityRegistry registry) {
        return new EntityModule(registry);
    }
}
