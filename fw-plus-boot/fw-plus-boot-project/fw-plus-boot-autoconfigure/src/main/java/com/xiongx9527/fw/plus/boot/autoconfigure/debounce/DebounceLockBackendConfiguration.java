package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.xiongx9527.fw.plus.common.debounce.DebounceBackendResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 防抖后端选择复用锁模块的统一旋钮 {@code fw.plus.lock.type}（与 {@code fw-plus-lock} 共用配置）：
 *
 * <ul>
 *   <li>{@code redis} → 使用 Redis 原子占位（跨实例严格只放行一次）；</li>
 *   <li>{@code jvm}   → 使用进程内 Spring Cache 占位（与 jvm 单实例语义一致）；</li>
 *   <li>{@code db} 或未显式配置 → 退化为历史默认：Redis 占位可用时优先 Redis，否则 Cache。</li>
 * </ul>
 *
 * <p>防抖自身没有 db 占位后端（其语义是「窗口内原子占位 + 自动过期」，与单 worker 守卫的
 * 持锁到显式 release 不同），故不强行并入 {@code fw-plus-lock} 的 {@link com.xiongx9527.fw.plus.lock.WorkerLock}，
 * 仅复用其「后端选择」这一统一入口。</p>
 */
@Configuration(proxyBeanMethods = false)
public class DebounceLockBackendConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.lock", name = "type", havingValue = "redis")
    public DebounceBackendResolver redisDebounceBackendResolver() {
        return () -> true;
    }

    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.lock", name = "type", havingValue = "jvm")
    public DebounceBackendResolver jvmDebounceBackendResolver() {
        return () -> false;
    }

    @Bean
    @ConditionalOnMissingBean(DebounceBackendResolver.class)
    public DebounceBackendResolver defaultDebounceBackendResolver() {
        // 与历史默认一致：有 Redis 占位实现时优先 Redis，否则 Spring Cache
        return () -> true;
    }
}
