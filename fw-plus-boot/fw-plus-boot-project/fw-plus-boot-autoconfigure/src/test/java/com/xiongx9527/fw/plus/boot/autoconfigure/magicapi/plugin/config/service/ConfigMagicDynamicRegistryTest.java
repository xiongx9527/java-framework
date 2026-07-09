package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ConfigMagicDynamicRegistryTest {

    @Test
    void shouldPublishConfigChangeEventAfterRegisterRefresh() {
        ConfigMagicDynamicRegistry registry = registry();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        ContextRefresher contextRefresher = mock(ContextRefresher.class);
        MockEnvironment environment = new MockEnvironment();
        ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
        ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
        ReflectionTestUtils.setField(registry, "environment", environment);

        ReflectionTestUtils.invokeMethod(registry, "registerByEntity", config("magic-api:\n  plugin:\n    mcp:\n      tools:\n        functions:\n          enabled: true\n"));

        verify(contextRefresher).refresh();
        assertThat(publisher.events).hasSize(1);
        ConfigChangeEvent event = publisher.events.get(0);
        assertThat(event.getProperties()).containsEntry("magic-api.plugin.mcp.tools.functions.enabled", true);
    }

    @Test
    void shouldPublishUnrelatedConfigChangeEventWithoutMcpBeans() {
        ConfigMagicDynamicRegistry registry = registry();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        ContextRefresher contextRefresher = mock(ContextRefresher.class);
        MockEnvironment environment = new MockEnvironment();
        ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
        ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
        ReflectionTestUtils.setField(registry, "environment", environment);

        ReflectionTestUtils.invokeMethod(registry, "registerByEntity", config("server:\n  port: 3090\n"));

        verify(contextRefresher).refresh();
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).getProperties()).containsEntry("server.port", 3090);
    }

    @Test
    void shouldPublishOriginalKeysWhenConfigIsUnregistered() {
        ConfigMagicDynamicRegistry registry = registry();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        ContextRefresher contextRefresher = mock(ContextRefresher.class);
        MockEnvironment environment = new MockEnvironment();
        ConfigInfo info = config("magic-api:\n  plugin:\n    mcp:\n      auth:\n        enabled: false\n");
        environment.getPropertySources().addFirst(new MapPropertySource("dev_app_mcp", java.util.Collections.<String, Object>singletonMap(
                "magic-api.plugin.mcp.auth.enabled", false)));
        ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
        ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
        ReflectionTestUtils.setField(registry, "environment", environment);

        ReflectionTestUtils.invokeMethod(registry, "unregisterByEntity", info);

        verify(contextRefresher).refresh();
        assertThat(environment.getPropertySources().contains("dev_app_mcp")).isFalse();
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).getProperties()).containsEntry("magic-api.plugin.mcp.auth.enabled", false);
    }

    @Test
    void shouldRefreshAndPublishOnlyOnceWhenConfigIsRegistered() {
        ConfigMagicDynamicRegistry registry = registry();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        ContextRefresher contextRefresher = mock(ContextRefresher.class);
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("dev_app_mcp", java.util.Collections.<String, Object>singletonMap(
                "server.port", 8080)));
        ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
        ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
        ReflectionTestUtils.setField(registry, "environment", environment);

        registry.register(config("server:\n  port: 3090\n"));

        verify(contextRefresher, times(1)).refresh();
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).getProperties()).containsEntry("server.port", 3090);
    }

    @Test
    void shouldPublishOriginalKeysWhenConfigIsDisabled() {
        ConfigMagicDynamicRegistry registry = registry();
        RecordingEventPublisher publisher = new RecordingEventPublisher();
        ContextRefresher contextRefresher = mock(ContextRefresher.class);
        MockEnvironment environment = new MockEnvironment();
        ConfigInfo info = config("magic-api:\n  plugin:\n    mcp:\n      headers:\n        session-id: Mcp-Session-Id\n");
        info.setEnabled(false);
        environment.getPropertySources().addFirst(new MapPropertySource("dev_app_mcp", java.util.Collections.<String, Object>singletonMap(
                "magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id")));
        ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
        ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
        ReflectionTestUtils.setField(registry, "environment", environment);

        ReflectionTestUtils.invokeMethod(registry, "registerByEntity", info);

        verify(contextRefresher).refresh();
        assertThat(environment.getPropertySources().contains("dev_app_mcp")).isFalse();
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.get(0).getProperties()).containsEntry("magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id");
    }

    private ConfigMagicDynamicRegistry registry() {
        return new ConfigMagicDynamicRegistry(null);
    }

    private ConfigInfo config(String script) {
        ConfigInfo info = new ConfigInfo();
        info.setEnv("dev");
        info.setAppId("app");
        info.setNamespace("mcp");
        info.setEnabled(true);
        info.setExternalFlag(false);
        info.setScript(script);
        return info;
    }

    private static class RecordingEventPublisher implements ApplicationEventPublisher {

        private final List<ConfigChangeEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof ConfigChangeEvent) {
                events.add((ConfigChangeEvent) event);
            }
        }
    }
}
