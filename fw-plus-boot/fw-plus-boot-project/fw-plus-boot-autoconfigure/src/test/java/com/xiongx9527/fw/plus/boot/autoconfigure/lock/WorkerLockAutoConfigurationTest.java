package com.xiongx9527.fw.plus.boot.autoconfigure.lock;

import com.xiongx9527.fw.plus.lock.DbWorkerLock;
import com.xiongx9527.fw.plus.lock.RedisWorkerLock;
import com.xiongx9527.fw.plus.lock.SingleWorkerGuard;
import com.xiongx9527.fw.plus.lock.WorkerLock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WorkerLockAutoConfiguration} 自动配置行为测试。
 */
class WorkerLockAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WorkerLockAutoConfiguration.class));

    @Test
    void dbBackendCreatedWhenJdbcTemplatePresent() {
        runner.withUserConfiguration(JdbcConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(WorkerLock.class);
                    assertThat(ctx.getBean(WorkerLock.class)).isInstanceOf(DbWorkerLock.class);
                    assertThat(ctx).hasSingleBean(SingleWorkerGuard.class);
                });
    }

    @Test
    void redisBackendCreatedWhenStringRedisTemplatePresent() {
        runner.withPropertyValues("fw.plus.lock.type=redis")
                .withUserConfiguration(RedisConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(WorkerLock.class);
                    assertThat(ctx.getBean(WorkerLock.class)).isInstanceOf(RedisWorkerLock.class);
                });
    }

    @Test
    void defaultDbBackendFailsWithoutJdbcTemplate() {
        // 缺省 type=db 但不存在 JdbcTemplate 时应启动失败（明确提示需要 JdbcTemplate）
        runner.run(ctx -> assertThat(ctx).hasFailed());
    }

    @Configuration
    static class JdbcConfig {
        @Bean
        public JdbcTemplate jdbcTemplate() {
            SimpleDriverDataSource ds = new SimpleDriverDataSource(
                    new org.h2.Driver(), "jdbc:h2:mem:workerlock;DB_CLOSE_DELAY=-1");
            return new JdbcTemplate(ds);
        }
    }

    @Configuration
    static class RedisConfig {
        @Bean
        public StringRedisTemplate stringRedisTemplate() {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }
}
