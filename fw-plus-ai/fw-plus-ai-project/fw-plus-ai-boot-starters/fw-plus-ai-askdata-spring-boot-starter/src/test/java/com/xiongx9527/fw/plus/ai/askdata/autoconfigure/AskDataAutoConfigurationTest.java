package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataAgentDispatcher;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataStepResult;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataSubAgent;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataSubAgentRequest;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataSubAgentResult;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorRetriever;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AskDataAutoConfigurationTest {

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
                    com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect dialect, int maxRows) {
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
            public AskDataResult ask(com.xiongx9527.fw.plus.ai.askdata.AskDataRequest request) {
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
        contextRunner
                .withBean(AskDataSubAgent.class, () -> subAgent)
                .withBean(AskDataMetadataProvider.class, () -> request -> new AskDataMetadata("mysql", null, null, null, null))
                .withBean(AskDataModelRegistry.class, () -> new AskDataModelRegistry() {
                    @Override
                    public AskDataChatModel getChatModel(String modelKey) {
                        return prompt -> new AskDataModelResponse("{\"sql\":\"select 1\"}", null, "test-model");
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
                })
                .run(context -> {
                    AskDataRequest request = new AskDataRequest();
                    request.setQuestion("订单数量");
                    request.setDatasourceKey("orders");
                    request.setExecute(Boolean.FALSE);

                    AskDataResult result = context.getBean(AskDataService.class).ask(request);

                    assertTrue(result.isSuccess());
                    assertTrue(subAgent.executed);
                });
    }

    private static class TestAskDataSubAgent implements AskDataSubAgent {

        private boolean executed;

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
            executed = true;
            return AskDataSubAgentResult.success("done");
        }
    }
}
