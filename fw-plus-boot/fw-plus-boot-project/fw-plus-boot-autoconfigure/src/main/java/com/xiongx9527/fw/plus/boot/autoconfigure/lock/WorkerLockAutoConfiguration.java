package com.xiongx9527.fw.plus.boot.autoconfigure.lock;

import com.xiongx9527.fw.plus.lock.DbWorkerLock;
import com.xiongx9527.fw.plus.lock.RedisWorkerLock;
import com.xiongx9527.fw.plus.lock.SingleWorkerGuard;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.lock.WorkerLock;
import com.xiongx9527.fw.plus.lock.WorkerLockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 单 worker 守卫的锁后端自动配置（{@code fw.plus.lock.type}）：
 * {@code db}（缺省，元数据库表行锁）/ {@code redis}（Redis SET NX PX）。
 *
 * <p>两个后端用互斥的 {@code @ConditionalOnProperty} 装配（各自带
 * {@code matchIfMissing}/{@code havingValue}），不依赖 {@code @ConditionalOnMissingBean} 的注册顺序。</p>
 *
 * <p>框架只提供能力，不强制开启任何后端：只有业务侧引入了 {@code spring-jdbc} 或
 * {@code spring-data-redis} 且未显式排除时才会装配对应实现；若所选后端缺少必需依赖，
 * 配置阶段即抛出清晰异常，避免静默失效。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WorkerLock.class)
@EnableConfigurationProperties(WorkerLockProperties.class)
public class WorkerLockAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WorkerLockAutoConfiguration.class);

    /** DB 版锁（缺省）。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fw.plus.lock", name = "type", havingValue = "db", matchIfMissing = true)
    public WorkerLock dbWorkerLock(WorkerLockProperties properties,
                                   ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.plus.lock.db.requireJdbcTemplate"));
        }
        log.info("装配单 worker 守卫锁后端：db");
        return new DbWorkerLock(jdbcTemplate, properties.getDebounce());
    }

    /** Redis 版锁。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fw.plus.lock", name = "type", havingValue = "redis")
    public WorkerLock redisWorkerLock(WorkerLockProperties properties,
                                      ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.plus.lock.redis.requireStringRedisTemplate"));
        }
        log.info("装配单 worker 守卫锁后端：redis");
        return new RedisWorkerLock(redisTemplate, properties.getPrefix(),
                properties.getTtl(), properties.getDebounce());
    }

    /** 单 worker 启动守卫（包裹锁后端，可被业务整体替换）。 */
    @Bean
    @ConditionalOnMissingBean
    public SingleWorkerGuard singleWorkerGuard(WorkerLock workerLock) {
        return new SingleWorkerGuard(workerLock);
    }
}
