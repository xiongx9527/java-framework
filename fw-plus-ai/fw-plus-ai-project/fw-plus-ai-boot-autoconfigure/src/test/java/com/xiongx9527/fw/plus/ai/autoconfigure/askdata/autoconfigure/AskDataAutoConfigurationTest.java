package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgentRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataSubAgentResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AskDataAutoConfigurationTest {

    private static final String SQL = "select count(*) from orders limit 100";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AskDataAutoConfiguration.class));

    @Test
    public void shouldRegisterAllDefaultAskDataBeans() {
        contextRunner.run(context -> {
            assertTrue(context.getBean(AskDataProperties.class) != null);
            assertTrue(context.getBean(AskDataModelRegistry.class) != null);
            assertTrue(context.getBean(AskDataVectorRetriever.class) != null);
            assertTrue(context.getBean(AskDataMetadataProvider.class) != null);
            assertTrue(context.getBean(AskDataPromptTemplateProvider.class) != null);
            assertTrue(context.getBean(AskDataSqlValidator.class) != null);
            assertTrue(context.getBean(AskDataSqlExecutor.class) != null);
            assertTrue(context.getBean(AskDataFlowExecutor.class) != null);
            assertTrue(context.getBean(AskDataConversationStore.class) != null);
            assertTrue(context.getBean(AskDataContextCompressor.class) != null);
            assertTrue(context.getBean(AskDataService.class) != null);
            assertTrue(context.getBean(AskDataDialectRegistry.class) != null);
            assertTrue(context.getBean(AskDataAgentDispatcher.class) != null);
        });
    }

    @Test
    public void shouldNotRegisterDefaultAskDataBeansWhenDisabled() {
        contextRunner.withPropertyValues("fw.plus.ai.askdata.enabled=false").run(context -> {
            assertTrue(!context.containsBean("askDataModelRegistry"));
            assertTrue(!context.containsBean("askDataService"));
        });
    }

    @Test
    public void shouldPreserveUserSqlValidator() {
        AskDataSqlValidator customValidator = new AskDataSqlValidator() {
            @Override
            public AskDataSqlValidationResult validate(String sql,
                    com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect dialect, int maxRows) {
                return AskDataSqlValidationResult.accepted(sql);
            }
        };
        contextRunner.withBean(AskDataSqlValidator.class, () -> customValidator).run(context -> {
            assertSame(customValidator, context.getBean(AskDataSqlValidator.class));
        });
    }

    @Test
    public void shouldPreserveUserAskDataService() {
        AskDataService customService = new AskDataService() {
            @Override
            public AskDataResult ask(com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest request) {
                return new AskDataResult();
            }
        };
        contextRunner.withBean(AskDataService.class, () -> customService).run(context -> {
            assertSame(customService, context.getBean(AskDataService.class));
        });
    }

    @Test
    public void shouldUseUserAskDataPropertiesForDefaultBeans() {
        AskDataProperties customProperties = new AskDataProperties();
        customProperties.getModel().setDefaultChatModel("custom-model");

        contextRunner.withBean(AskDataProperties.class, () -> customProperties).run(context -> {
            assertTrue(context.getStartupFailure() == null);
            assertSame(customProperties, context.getBean(AskDataProperties.class));
            assertTrue("custom-model".equals(context.getBean(AskDataModelRegistry.class).getDefaultChatModelKey()));
        });
    }

    @Test
    public void shouldDispatchUserSubAgentThroughDefaultFlow() {
        TestAskDataSubAgent subAgent = new TestAskDataSubAgent();
        withDefaultFlowDependencies()
                .withBean(AskDataSubAgent.class, () -> subAgent)
                .run(context -> {
                    AskDataResult result = context.getBean(AskDataService.class).ask(createRequest());

                    assertTrue(result.isSuccess());
                    assertTrue(subAgent.executed);
                    assertTrue(hasStep(result, "sub-agent:test-sub-agent"));
                    assertTrue(hasStep(result, "merge-sub-agents"));
                    assertTrue(context.getBean(AskDataConversationStore.class)
                            .loadLatest(result.getConversationId()) != null);
                });
    }

    @Test
    public void shouldExecuteAndRecordCustomMainAgent() {
        AskDataAgent mainAgent = new AskDataAgent() {
            @Override
            public String getName() {
                return "custom-main";
            }

            @Override
            public AskDataStepResult execute(AskDataStepContext context) {
                context.getAttributes().put("custom-main", "ready");
                return AskDataStepResult.success("main output");
            }
        };
        withDefaultFlowDependencies()
                .withBean(AskDataAgent.class, () -> mainAgent)
                .run(context -> {
                    AskDataResult result = context.getBean(AskDataService.class).ask(createRequest());

                    assertTrue(result.isSuccess());
                    assertTrue(hasStep(result, "agent:custom-main"));
                });
    }

    @Test
    public void shouldGenerateSqlWhenSubAgentReturnsFailure() {
        FailureAskDataSubAgent subAgent = new FailureAskDataSubAgent();
        withDefaultFlowDependencies()
                .withBean(AskDataSubAgent.class, () -> subAgent)
                .run(context -> {
                    assertSuccessfulSql(context.getBean(AskDataService.class).ask(createRequest()));
                    assertEquals(1, subAgent.getExecuteSubTaskInvocations());
                });
    }

    @Test
    public void shouldGenerateSqlWhenSubAgentThrowsRuntimeException() {
        ThrowingAskDataSubAgent subAgent = new ThrowingAskDataSubAgent();
        withDefaultFlowDependencies()
                .withBean(AskDataSubAgent.class, () -> subAgent)
                .run(context -> {
                    assertSuccessfulSql(context.getBean(AskDataService.class).ask(createRequest()));
                    assertEquals(1, subAgent.getExecuteSubTaskInvocations());
                });
    }

    @Test
    public void shouldGenerateSqlWhenNoSubAgentIsRegistered() {
        withDefaultFlowDependencies()
                .run(context -> assertSuccessfulSql(context.getBean(AskDataService.class).ask(createRequest())));
    }

    private ApplicationContextRunner withDefaultFlowDependencies() {
        return contextRunner
                .withBean(AskDataMetadataProvider.class,
                        () -> request -> new AskDataMetadata("mysql", null, null, null, null))
                .withBean(AskDataModelRegistry.class, () -> new AskDataModelRegistry() {
                    @Override
                    public AskDataChatModel getChatModel(String modelKey) {
                        return prompt -> new AskDataModelResponse("{\"sql\":\"" + SQL + "\"}", null, "test-model");
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
                        return "test-embedding-model";
                    }
                });
    }

    private AskDataRequest createRequest() {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单数量");
        request.setDatasourceKey("orders");
        request.setExecute(Boolean.FALSE);
        return request;
    }

    private void assertSuccessfulSql(AskDataResult result) {
        assertTrue(result.isSuccess());
        assertEquals(SQL, result.getSql());
    }

    private boolean hasStep(AskDataResult result, String stepKey) {
        for (com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog step : result.getSteps()) {
            if (stepKey.equals(step.getStepKey())) {
                return true;
            }
        }
        return false;
    }

    private static class TestAskDataSubAgent implements AskDataSubAgent {

        private boolean executed;
        private int executeSubTaskInvocations;

        @Override
        public String getName() {
            return "test-sub-agent";
        }

        @Override
        public AskDataStepResult execute(AskDataStepContext context) {
            return null;
        }

        @Override
        public AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request) {
            recordExecuteSubTaskInvocation();
            executed = true;
            return AskDataSubAgentResult.success("done");
        }

        protected void recordExecuteSubTaskInvocation() {
            executeSubTaskInvocations++;
        }

        protected int getExecuteSubTaskInvocations() {
            return executeSubTaskInvocations;
        }
    }

    private static class FailureAskDataSubAgent extends TestAskDataSubAgent {

        @Override
        public AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request) {
            recordExecuteSubTaskInvocation();
            return AskDataSubAgentResult.failure("sub-agent failed");
        }
    }

    private static class ThrowingAskDataSubAgent extends TestAskDataSubAgent {

        @Override
        public AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request) {
            recordExecuteSubTaskInvocation();
            throw new RuntimeException("sub-agent failed");
        }
    }
}
