package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceSlot;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("enabled=true：注册切面与防抖器；无 StringRedisTemplate 时降级 Cache 后端")
    void enabledRegistersAspectAndFallsBackToCache() {
        runner.withPropertyValues("fw.plus.debounce.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(DebounceAspect.class);
            assertThat(context).hasSingleBean(RequestDebouncer.class);
            assertThat(context).doesNotHaveBean(DebounceSlot.class);
        });
    }
}
