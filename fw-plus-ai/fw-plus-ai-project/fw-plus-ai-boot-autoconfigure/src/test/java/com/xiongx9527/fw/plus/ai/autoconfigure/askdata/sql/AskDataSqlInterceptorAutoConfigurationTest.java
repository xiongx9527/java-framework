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

    private AnnotationConfigApplicationContext context(Class<?> interceptorConfiguration) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SafetyValidatorConfiguration.class, interceptorConfiguration, AskDataAutoConfiguration.class);
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
