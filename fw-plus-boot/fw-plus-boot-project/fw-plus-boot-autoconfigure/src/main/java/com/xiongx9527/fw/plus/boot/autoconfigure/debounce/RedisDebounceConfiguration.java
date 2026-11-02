package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 防抖原子占位装配：仅在 classpath 存在 spring-data-redis 且容器里有
 * {@link StringRedisTemplate} 时生效。
 *
 * <p>独立成类而不是内部类，由 {@link DebounceAutoConfiguration} 通过 {@code @Import} 引入：
 * 条件不满足时 Spring 只读取类元数据（ASM）即跳过，不会加载 Redis 相关类型，因此未引入
 * spring-data-redis 的应用也能正常启动并降级为 Cache 后端。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisDebounceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisDebounceSlot redisDebounceSlot(StringRedisTemplate redisTemplate) {
        return new RedisDebounceSlot(redisTemplate);
    }
}
