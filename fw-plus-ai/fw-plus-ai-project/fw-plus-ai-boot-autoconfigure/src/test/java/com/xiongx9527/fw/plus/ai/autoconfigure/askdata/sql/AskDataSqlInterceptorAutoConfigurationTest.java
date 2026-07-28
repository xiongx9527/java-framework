package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AskDataSqlInterceptorAutoConfigurationTest {

    @Test
    public void shouldBridgeSpringInterceptorWithTenantNamedParameter() {
        AnnotationConfigApplicationContext context = context(TenantInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            AskDataSqlCommand rewritten = chain.execute(command("select * from orders"));

            assertEquals("select * from orders where tenant_id = :tenantId", rewritten.getSql());
            assertEquals("tenant-1", rewritten.getParameter("tenantId").getValue());
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldRejectUnsafeSqlAfterPluginRewrite() {
        AnnotationConfigApplicationContext context = context(UnsafeInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            try {
                chain.execute(command("select * from orders"));
                fail("恶意插件改写后的写入 SQL 必须由最终校验拒绝");
            } catch (IllegalArgumentException ex) {
                assertTrue(ex.getMessage().contains("WRITE_SQL"));
            }
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldAuditInterceptorKeyAndFinalValidationPhase() {
        AnnotationConfigApplicationContext context = context(TenantInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            chain.execute(command("select * from orders"));

            List<AskDataSqlAudit.Record> records = context.getBean(AskDataSqlAudit.class).getRecords();
            assertEquals(2, records.size());
            assertEquals("tenant-predicate", records.get(0).getInterceptorKey());
            assertEquals("INTERCEPT", records.get(0).getPhase());
            assertEquals("validator", records.get(1).getInterceptorKey());
            assertEquals("FINAL_VALIDATE", records.get(1).getPhase());
        } finally {
            context.close();
        }
    }

    private AnnotationConfigApplicationContext context(Class<?> interceptorConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SafetyValidatorConfiguration.class, interceptorConfiguration, AskDataAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private AskDataSqlCommand command(String sql) {
        return new AskDataSqlCommand(new AskDataProjectContext("project-1", "operator-1"), sql,
                Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-1")),
                Collections.singleton("orders"));
    }

    @Configuration
    static class SafetyValidatorConfiguration {

        @Bean
        public com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreAskDataSqlValidator() {
            return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator() {
                @Override
                public Result validate(AskDataSqlCommand command) {
                    if (command.getSql().toLowerCase().contains("delete")) {
                        return Result.failure(FailureCategory.WRITE_SQL, "只允许只读 SQL");
                    }
                    return Result.success();
                }
            };
        }
    }

    @Configuration
    static class TenantInterceptorConfiguration {

        @Bean
        @Order(10)
        public AskDataSqlInterceptor tenantPredicateInterceptor() {
            return new AskDataSqlInterceptor() {
                @Override
                public String getKey() {
                    return "tenant-predicate";
                }

                @Override
                public int getOrder() {
                    return 10;
                }

                @Override
                public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                    if (command.getParameter("tenantId") == null) {
                        throw new IllegalArgumentException("缺少 tenantId 命名参数");
                    }
                    return command.withSql(command.getSql() + " where tenant_id = :tenantId");
                }
            };
        }
    }

    @Configuration
    static class UnsafeInterceptorConfiguration {

        @Bean
        @Order(10)
        public AskDataSqlInterceptor unsafeInterceptor() {
            return new AskDataSqlInterceptor() {
                @Override
                public String getKey() {
                    return "unsafe-rewrite";
                }

                @Override
                public int getOrder() {
                    return 10;
                }

                @Override
                public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                    return command.withSql("delete from orders");
                }
            };
        }
    }
}
