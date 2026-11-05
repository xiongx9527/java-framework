package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceSlot;
import org.junit.jupiter.api.DisplayName;
import com.xiongx9527.fw.plus.common.debounce.DebounceBackendResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/** 装配契约：缺省关闭时不注册切面，但配置属性始终可注入。 */
class DebounceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DebouncePropertiesConfiguration.class,
                    DebounceAutoConfiguration.class,
                    RedisDebounceConfiguration.class))
            .withBean(CacheManager.class, ConcurrentMapCacheManager::new)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("缺省关闭：不注册切面 / 防抖器 / Redis 占位，但属性仍可注入")
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DebounceProperties.class);
            assertThat(context).doesNotHaveBean(DebounceAspect.class);
            assertThat(context).doesNotHaveBean(RequestDebouncer.class);
            assertThat(context).doesNotHaveBean(DebounceSlot.class);
        });
    }

    @Test
    @DisplayName("fw.plus.lock.type=jvm：防抖后端解析为进程内 Cache（useRedis=false）")
    void lockTypeJvmSelectsCacheBackend() {
        runner.withPropertyValues("fw.plus.debounce.enabled=true", "fw.plus.lock.type=jvm")
                .run(context -> {
                    assertThat(context).hasSingleBean(DebounceBackendResolver.class);
                    assertThat(context.getBean(DebounceBackendResolver.class).useRedis()).isFalse();
                });
    }

    @Test
    @DisplayName("fw.plus.lock.type=redis：防抖后端解析为 Redis 原子占位（useRedis=true）")
    void lockTypeRedisSelectsRedisBackend() {
        runner.withPropertyValues("fw.plus.debounce.enabled=true", "fw.plus.lock.type=redis")
                .run(context -> {
                    assertThat(context).hasSingleBean(DebounceBackendResolver.class);
                    assertThat(context.getBean(DebounceBackendResolver.class).useRedis()).isTrue();
                });
    }

    @Test
    @DisplayName("enabled=true：注册切面与防抖器；无 StringRedisTemplate 时降级 Cache 后端")
    void enabledRegistersAspectAndFallsBackToCache() {
        runner.withPropertyValues("fw.plus.debounce.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(DebounceAspect.class);
            assertThat(context).hasSingleBean(RequestDebouncer.class);
            assertThat(context).doesNotHaveBean(DebounceSlot.class);
        });
    }
}
