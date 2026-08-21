package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module.MqttModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIMqttConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIMqttConfiguration.class))
            .withUserConfiguration(MagicApiSupportConfiguration.class);

    @Test
    void mqttServerPluginBacksOffWhenPropertyIsMissing() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(MqttModule.class)
                .doesNotHaveBean(BaseMagicDynamicMqttDataSource.class));
    }

    @Test
    void mqttServerPluginLoadsWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("magic-api.plugin.mqtt.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(MqttModule.class)
                        .hasSingleBean(BaseMagicDynamicMqttDataSource.class));
    }

    @Test
    void mqttDashboardAdminFlagDefaultsToFalseAndFollowsApplicationConfig() {
        MagicAPIMqttConfiguration configuration = new MagicAPIMqttConfiguration(new MagicAPIProperties());
        assertThat(configuration.plugin().getAdminEnabled()).isFalse();

        StaticApplicationContext context = new StaticApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("mqtt-dashboard-test",
                Collections.<String, Object>singletonMap("magic-api.plugin.mqtt.admin.enabled", "true")));
        new SpringUtils().setApplicationContext(context);
        try {
            Plugin plugin = configuration.plugin();
            assertThat(plugin.getName()).isEqualTo("mqtt");
            assertThat(plugin.getAdminEnabled()).isTrue();
        } finally {
            context.close();
            new SpringUtils().setApplicationContext(null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MagicApiSupportConfiguration {

        @Bean
        MagicAPIProperties magicAPIProperties() {
            return new MagicAPIProperties();
        }

        @Bean
        MagicResourceService magicResourceService() {
            return mock(MagicResourceService.class);
        }
    }
}
