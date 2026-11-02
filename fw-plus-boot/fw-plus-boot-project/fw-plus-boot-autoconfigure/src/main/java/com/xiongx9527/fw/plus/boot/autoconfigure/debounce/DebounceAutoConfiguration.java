package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.boot.common.debounce.DebounceAspect;
import com.xiongx9527.fw.plus.boot.common.debounce.RedisDebounceSlot;
import com.xiongx9527.fw.plus.boot.common.debounce.RequestDebouncer;
import com.xiongx9527.fw.plus.common.debounce.DebounceBackendResolver;
import com.xiongx9527.fw.plus.common.debounce.DebounceKeyBuilder;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceUserResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 接口防抖自动配置：{@code @Debounce} 注解 + 键构造 + 原子占位 + AOP 切面。
 *
 * <p>装配约定：</p>
 * <ul>
 *   <li>默认使用 Spring Cache 后端；存在 {@link StringRedisTemplate} 时默认优先 Redis（跨实例）；</li>
 *   <li>业务侧可提供 {@link DebounceBackendResolver} 覆盖「何时用 Redis」；</li>
 *   <li>业务侧可提供 {@link DebounceUserResolver} 提供当前用户；缺省按匿名处理；</li>
 *   <li>所有 bean 均 {@code @ConditionalOnMissingBean}，业务侧可完全接管其中任一组件。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {
        "org.aspectj.lang.annotation.Aspect",
        "org.springframework.cache.CacheManager",
        "com.fasterxml.jackson.databind.ObjectMapper"
})
@EnableConfigurationProperties(DebounceProperties.class)
public class DebounceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DebounceKeyBuilder debounceKeyBuilder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new DebounceKeyBuilder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public DebounceUserResolver debounceUserResolver() {
        return () -> null;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CacheManager.class)
    public RequestDebouncer requestDebouncer(CacheManager cacheManager,
                                             DebounceProperties properties,
                                             ObjectProvider<RedisDebounceSlot> redisSlotProvider,
                                             ObjectProvider<DebounceBackendResolver> backendResolverProvider) {
        return new RequestDebouncer(cacheManager, properties, redisSlotProvider, backendResolverProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RequestDebouncer.class)
    public DebounceAspect debounceAspect(RequestDebouncer debouncer,
                                         DebounceProperties properties,
                                         DebounceKeyBuilder keyBuilder,
                                         DebounceUserResolver userResolver) {
        return new DebounceAspect(debouncer, properties, keyBuilder, userResolver);
    }

    /**
     * Redis 原子占位（仅在 spring-data-redis 与 StringRedisTemplate 均存在时装配）。
     * 单独成内部类，避免未引入 Redis 的应用加载到 Redis 相关类型。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    static class RedisDebounceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(StringRedisTemplate.class)
        public RedisDebounceSlot redisDebounceSlot(StringRedisTemplate redisTemplate) {
            return new RedisDebounceSlot(redisTemplate);
        }
    }
}
