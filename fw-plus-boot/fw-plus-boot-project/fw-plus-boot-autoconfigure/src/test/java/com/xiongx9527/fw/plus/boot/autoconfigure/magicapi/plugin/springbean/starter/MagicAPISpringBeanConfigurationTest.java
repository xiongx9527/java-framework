package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.module.SpringBeanModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service.SpringBeanMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service.SpringBeanMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.starter.magicapi.springbean.SpringBeanPluginMarker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicAPISpringBeanConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPISpringBeanConfiguration.class));

    @Test
    void createsSpringBeanPluginBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SpringBeanProperties.class);
            assertThat(context).hasSingleBean(DynamicSpringBeanRegistry.class);
            assertThat(context).hasSingleBean(SpringBeanMagicResourceStorage.class);
            assertThat(context).hasSingleBean(SpringBeanMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(SpringBeanModule.class);
        });
    }

    @Test
    void canBeDisabledExplicitlyOrByLeavingOutTheIndependentPlugin() {
        contextRunner.withPropertyValues("magic-api.plugin.spring-bean.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DynamicSpringBeanRegistry.class));

        contextRunner.withClassLoader(new FilteredClassLoader(SpringBeanPluginMarker.class))
                .run(context -> assertThat(context).doesNotHaveBean(DynamicSpringBeanRegistry.class));
    }

    @Test
    void registersConsolePluginMetadata() {
        MagicPluginConfiguration configuration = new MagicAPISpringBeanConfiguration();
        Plugin plugin = configuration.plugin();

        assertEquals("spring-bean", plugin.getName());
        assertEquals("MagicSpringBean", plugin.getGlobalName());
        assertEquals("magic-spring-bean.1.0.0.iife.js", plugin.getJavascriptFilename());
    }
}
