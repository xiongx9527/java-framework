package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.common.debounce.DebounceBackendResolver;
import com.xiongx9527.fw.plus.common.debounce.DebounceKeyBuilder;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceSlot;
import com.xiongx9527.fw.plus.common.debounce.DebounceUserResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 接口防抖自动配置：{@code @Debounce} 注解 + 键构造 + 原子占位 + AOP 切面。
 *
 * <p>装配约定：</p>
 * <ul>
 *   <li>默认使用 Spring Cache 后端；存在 Redis 占位实现时默认优先 Redis（跨实例）；</li>
 *   <li>业务侧可提供 {@link DebounceBackendResolver} 覆盖「何时用 Redis」；</li>
 *   <li>业务侧可提供 {@link DebounceUserResolver} 提供当前用户；缺省按匿名处理；</li>
 *   <li>所有 bean 均 {@code @ConditionalOnMissingBean}，业务侧可完全接管其中任一组件；</li>
 *   <li>仅在 {@code fw.plus.debounce.enabled=true} 时装配（缺省关闭，不注册切面）。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {
        "org.aspectj.lang.annotation.Aspect",
        "org.springframework.cache.CacheManager",
        "com.fasterxml.jackson.databind.ObjectMapper"
})
@ConditionalOnProperty(prefix = DebounceProperties.PREFIX, name = "enabled", havingValue = "true")
@Import({RedisDebounceConfiguration.class, DebounceLockBackendConfiguration.class})
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
                                             ObjectProvider<DebounceSlot> slotProvider,
                                             ObjectProvider<DebounceBackendResolver> backendResolverProvider) {
        return new RequestDebouncer(cacheManager, properties, slotProvider, backendResolverProvider);
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
}
