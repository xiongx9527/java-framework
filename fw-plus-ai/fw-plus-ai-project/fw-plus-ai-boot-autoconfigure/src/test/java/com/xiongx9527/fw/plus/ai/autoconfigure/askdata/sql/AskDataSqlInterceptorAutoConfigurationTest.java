package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void shouldCreateInterceptorChainWithDefaultCoreValidator() {
        AnnotationConfigApplicationContext context = contextWithDefaultCoreValidator(TenantInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            assertEquals("select * from orders where tenant_id = :tenantId",
                    chain.execute(command("select * from orders")).getSql());
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldRejectTableAccessWhenDefaultCoreValidatorHasNoAuthorizedObjects() {
        AnnotationConfigApplicationContext context = contextWithDefaultCoreValidator(NoopInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> chain.execute(new AskDataSqlCommand(new AskDataProjectContext("project-1", "operator-1"),
                            "select * from/*scope*/orders", Collections.<AskDataSqlParameter>emptyList(),
                            Collections.<String>emptySet())));
            assertTrue(exception.getMessage().contains("UNAUTHORIZED_OBJECT"));
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

    @Test
    public void shouldApplyCoreOrderingAcrossMultipleSpringInterceptors() {
        AnnotationConfigApplicationContext context = context(OrderedInterceptorConfiguration.class);
        try {
            AskDataSqlInterceptorChain chain = context.getBeanProvider(AskDataSqlInterceptorChain.class)
                    .getIfAvailable();

            assertNotNull(chain);
            AskDataSqlCommand rewritten = chain.execute(command("select * from orders"));

            assertEquals("select * from orders /* a-first */ /* b-same-order */ /* z-later */", rewritten.getSql());
            List<AskDataSqlAudit.Record> records = context.getBean(AskDataSqlAudit.class).getRecords();
            assertEquals(4, records.size());
            assertEquals("a-first", records.get(0).getInterceptorKey());
            assertEquals("b-same-order", records.get(1).getInterceptorKey());
            assertEquals("z-later", records.get(2).getInterceptorKey());
            assertEquals("FINAL_VALIDATE", records.get(3).getPhase());
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldExecuteFlowWithSqlRewrittenByCoreInterceptorChain() {
        AnnotationConfigApplicationContext context = flowContext(TenantInterceptorConfiguration.class);
        try {
            AskDataResult result = context.getBean(AskDataFlowExecutor.class).execute(request());

            assertTrue(result.isSuccess());
            assertEquals("select * from orders where tenant_id = :tenantId", result.getSql());
            assertEquals(result.getSql(), context.getBean(CapturingSqlExecutor.class).getExecutedSql());
            List<AskDataSqlAudit.Record> records = context.getBean(AskDataSqlAudit.class).getRecords();
            assertEquals(2, records.size());
            assertEquals("tenant-predicate", records.get(0).getInterceptorKey());
            assertEquals("FINAL_VALIDATE", records.get(1).getPhase());
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldRejectUnsafePluginRewriteBeforeFlowExecutesSql() {
        AnnotationConfigApplicationContext context = flowContext(UnsafeInterceptorConfiguration.class);
        try {
            AskDataResult result = context.getBean(AskDataFlowExecutor.class).execute(request());

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("WRITE_SQL"));
            assertNull(context.getBean(CapturingSqlExecutor.class).getExecutedSql());
        } finally {
            context.close();
        }
    }

    @Test
    public void shouldRunBootValidatorAgainAfterCorePluginRewrite() {
        AnnotationConfigApplicationContext context = flowContextWithCustomCoreValidator(
                PermissiveCoreValidatorConfiguration.class, ForbiddenInterceptorConfiguration.class);
        try {
            AskDataResult result = context.getBean(AskDataFlowExecutor.class).execute(request());

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("metadata denied"));
            assertNull(context.getBean(CapturingSqlExecutor.class).getExecutedSql());
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

    private AnnotationConfigApplicationContext contextWithDefaultCoreValidator(Class<?> interceptorConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(interceptorConfiguration, AskDataAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private AnnotationConfigApplicationContext flowContext(Class<?> interceptorConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SafetyValidatorConfiguration.class, FlowConfiguration.class, interceptorConfiguration,
                AskDataAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private AnnotationConfigApplicationContext flowContextWithCustomCoreValidator(Class<?> coreValidatorConfiguration,
            Class<?> interceptorConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(coreValidatorConfiguration, FlowConfiguration.class, interceptorConfiguration,
                AskDataAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private AskDataRequest request() {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单查询");
        request.setProjectContext(new AskDataProjectContext("project-1", "operator-1"));
        request.setDatasourceKey("orders");
        request.setDialect("mysql");
        request.setModelKey("test-model");
        request.setExecute(Boolean.TRUE);
        request.setOptions(Collections.<String, Object>singletonMap("tenantId", "tenant-1"));
        return request;
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
    static class NoopInterceptorConfiguration {

        @Bean
        public AskDataSqlInterceptor noopInterceptor() {
            return new AskDataSqlInterceptor() {
                @Override
                public String getKey() {
                    return "noop";
                }

                @Override
                public int getOrder() {
                    return 1;
                }

                @Override
                public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                    return command;
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

    @Configuration
    static class ForbiddenInterceptorConfiguration {

        @Bean
        @Order(10)
        public AskDataSqlInterceptor forbiddenInterceptor() {
            return new AskDataSqlInterceptor() {
                @Override
                public String getKey() {
                    return "forbidden-rewrite";
                }

                @Override
                public int getOrder() {
                    return 10;
                }

                @Override
                public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                    return command.withSql("select * from forbidden");
                }
            };
        }
    }

    @Configuration
    static class PermissiveCoreValidatorConfiguration {

        @Bean
        public com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreAskDataSqlValidator() {
            return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator() {
                @Override
                public Result validate(AskDataSqlCommand command) {
                    return Result.success();
                }
            };
        }
    }

    @Configuration
    static class OrderedInterceptorConfiguration {

        @Bean
        @Order(100)
        public AskDataSqlInterceptor aFirstInterceptor() {
            return orderedInterceptor("a-first", 10, " /* a-first */");
        }

        @Bean
        @Order(0)
        public AskDataSqlInterceptor bSameOrderInterceptor() {
            return orderedInterceptor("b-same-order", 10, " /* b-same-order */");
        }

        @Bean
        @Order(-100)
        public AskDataSqlInterceptor zLaterInterceptor() {
            return orderedInterceptor("z-later", 20, " /* z-later */");
        }

        private AskDataSqlInterceptor orderedInterceptor(final String key, final int order, final String suffix) {
            return new AskDataSqlInterceptor() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public int getOrder() {
                    return order;
                }

                @Override
                public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                    return command.withSql(command.getSql() + suffix);
                }
            };
        }
    }

    @Configuration
    static class FlowConfiguration {

        @Bean
        public AskDataProperties askDataProperties() {
            AskDataProperties properties = new AskDataProperties();
            properties.getSql().setExecuteEnabled(true);
            return properties;
        }

        @Bean
        public AskDataModelRegistry askDataModelRegistry() {
            return new AskDataModelRegistry() {
                @Override
                public AskDataModelResponse generate(AskDataRequest request, String prompt) {
                    return new AskDataModelResponse("{\"sql\":\"select * from orders\"}", "raw", "test-model");
                }

                @Override
                public AskDataChatModel getChatModel(String modelKey) {
                    return null;
                }

                @Override
                public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
                    return null;
                }

                @Override
                public String getDefaultChatModelKey() {
                    return "test-model";
                }

                @Override
                public String getDefaultEmbeddingModelKey() {
                    return null;
                }
            };
        }

        @Bean
        public AskDataMetadataProvider askDataMetadataProvider() {
            return new AskDataMetadataProvider() {
                @Override
                public AskDataMetadata load(AskDataRequest request) {
                    return new AskDataMetadata("mysql",
                            Collections.singletonList(new AskDataTable("orders", Collections.emptyList())),
                            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                }
            };
        }

        @Bean
        public AskDataGovernanceProvider askDataGovernanceProvider() {
            return request -> AskDataAuthorizationSummary.allow(Collections.singletonList(
                    new AskDataAuthorizedObject("orders", "orders", "*")), Collections.emptyList());
        }

        @Bean
        public AskDataVectorRetriever askDataVectorRetriever() {
            return new AskDataVectorRetriever() {
                @Override
                public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                    return Collections.emptyList();
                }
            };
        }

        @Bean
        public AskDataPromptTemplateProvider askDataPromptTemplateProvider() {
            return new AskDataPromptTemplateProvider() {
                @Override
                public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                        List<AskDataRetrievedDocument> documents, int maxRows) {
                    return "prompt";
                }
            };
        }

        @Bean
        public AskDataSqlValidator askDataSqlValidator() {
            return new AskDataSqlValidator() {
                @Override
                public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                    return validate(sql, dialect, maxRows, null);
                }

                @Override
                public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows,
                        AskDataMetadata metadata) {
                    if (sql != null && sql.contains("forbidden")) {
                        return AskDataSqlValidationResult.rejected("SQL_REJECTED", "metadata denied");
                    }
                    return AskDataSqlValidationResult.accepted(sql);
                }
            };
        }

        @Bean
        public CapturingSqlExecutor askDataSqlExecutor() {
            return new CapturingSqlExecutor();
        }
    }

    static class CapturingSqlExecutor implements AskDataSqlExecutor {

        private String executedSql;

        @Override
        public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
            executedSql = sql;
            return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
        }

        String getExecutedSql() {
            return executedSql;
        }
    }
}
