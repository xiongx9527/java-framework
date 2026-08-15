package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.module.EntityModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.starter.MagicAPISpringBeanConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.MagicAPIWebFluxAutoConfiguration;
import com.xiongx9527.fw.plus.boot.starter.magicapi.javaentity.JavaEntityPluginMarker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MagicAPIJavaEntityConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIJavaEntityConfiguration.class));

    @Test
    void createsPluginBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryJavaEntityCompiler.class);
            assertThat(context).hasSingleBean(JavaEntityProperties.class);
            assertThat(context).hasSingleBean(DynamicEntityRegistry.class);
            assertThat(context).hasSingleBean(JavaEntityMagicResourceStorage.class);
            assertThat(context).hasSingleBean(JavaEntityMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(EntityModule.class);
        });
    }

    @Test
    void bindsTheConfiguredCompilationConcurrencyLimit() {
        contextRunner.withPropertyValues("magic-api.plugin.java-entity.max-concurrent-compilations=2")
                .run(context -> assertThat(context.getBean(JavaEntityProperties.class)
                        .getMaxConcurrentCompilations()).isEqualTo(2));
    }

    @Test
    void canBeDisabledExplicitlyOrByLeavingOutTheIndependentPlugin() {
        contextRunner.withPropertyValues("magic-api.plugin.java-entity.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DynamicEntityRegistry.class));

        contextRunner.withClassLoader(new FilteredClassLoader(JavaEntityPluginMarker.class))
                .run(context -> assertThat(context).doesNotHaveBean(DynamicEntityRegistry.class));
    }

    @Test
    void missingSystemCompilerDoesNotPreventContextStartup() {
        contextRunner.withBean("inMemoryJavaEntityCompiler", InMemoryJavaEntityCompiler.class,
                        () -> new InMemoryJavaEntityCompiler(null, getClass().getClassLoader()))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EntityModule.class);
                    assertFalse(context.getBean(DynamicEntityRegistry.class).status().isAvailable());
                });
    }

    @Test
    void coexistsWithTheSpringBeanPluginWithoutAmbiguousCompilerDependencies() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MagicAPIJavaEntityConfiguration.class,
                        MagicAPISpringBeanConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DynamicEntityRegistry.class);
                    assertThat(context).hasSingleBean(DynamicSpringBeanRegistry.class);
                });
    }

    @Test
    void webFluxAutoConfigurationImportsTheJavaEntityPlugin() {
        Import imports = MagicAPIWebFluxAutoConfiguration.class.getAnnotation(Import.class);

        assertThat(imports).isNotNull();
        assertThat(imports.value()).contains(MagicAPIJavaEntityConfiguration.class);
    }
}
