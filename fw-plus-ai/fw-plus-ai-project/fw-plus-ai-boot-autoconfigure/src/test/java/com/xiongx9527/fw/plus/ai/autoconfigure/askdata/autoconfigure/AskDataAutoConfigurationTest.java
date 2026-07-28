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
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
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
import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataAutoConfigurationTest {

    private static final String SQL = "select count(*) from orders limit 100";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LangChain4jModelAskDataAutoConfiguration.class, AskDataAutoConfiguration.class));

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
            assertTrue(context.getBean(com.xiongx9527.fw.plus.ai.askdata.api.AskDataService.class) != null);
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
    public void shouldRejectDefaultVectorRetrieverWhenClusterModeIsEnabled() {
        AskDataProperties properties = new AskDataProperties();
        properties.getState().setClusterMode(true);
        properties.getState().setStore("redis");
        properties.getVector().setStore("memory");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new AskDataAutoConfiguration().askDataVectorRetriever(properties,
                        new DefaultListableBeanFactory().getBeanProvider(ContentRetriever.class)));

        assertTrue(exception.getMessage().contains("向量"));
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
    public void shouldExposeCoreAskDataServiceAndPreferBusinessImplementation() {
        com.xiongx9527.fw.plus.ai.askdata.api.AskDataService customService =
                new com.xiongx9527.fw.plus.ai.askdata.api.AskDataService() {
                    @Override
                    public com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult ask(
                            com.xiongx9527.fw.plus.ai.askdata.api.AskDataRequest request) {
                        return com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult.success("业务实现");
                    }
                };

        contextRunner.withBean(com.xiongx9527.fw.plus.ai.askdata.api.AskDataService.class, () -> customService)
                .run(context -> assertSame(customService,
                        context.getBean(com.xiongx9527.fw.plus.ai.askdata.api.AskDataService.class)));
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
    public void shouldUseDirectLangChain4jBeansWithoutOptionalModelCore() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.model"))
                .withPropertyValues(
                        "fw.plus.ai.askdata.model.default-chat-model=directChat",
                        "fw.plus.ai.askdata.model.default-embedding-model=directEmbedding")
                .withBean("directChat", ChatLanguageModel.class, () -> chatModel("direct-response"))
                .withBean("directEmbedding", EmbeddingModel.class, () -> embeddingModel(0.3F, 0.7F))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    AskDataModelRegistry registry = context.getBean(AskDataModelRegistry.class);
                    assertEquals("direct-response", registry.getChatModel(null).generate("question").getText());
                    assertEquals(java.util.Arrays.asList(0.3F, 0.7F), registry.getEmbeddingModel(null).embed("question"));
                });
    }

    @Test
    public void shouldUseModelsFromOptionalModelRegistry() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put("managed", new LangChain4jModel("test", LangChain4jModelType.CHAT,
                chatModel("managed-response")));
        LangChain4jModelRegistry modelRegistry = new DefaultLangChain4jModelRegistry(models, "managed");

        contextRunner
                .withPropertyValues("fw.plus.ai.askdata.model.default-chat-model=managed")
                .withBean(LangChain4jModelRegistry.class, () -> modelRegistry)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertEquals("managed-response", context.getBean(AskDataModelRegistry.class)
                            .getChatModel(null).generate("question").getText());
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
                    java.util.List<com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog> logs =
                            context.getBean(com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.class)
                                    .list(new com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext("default", "default"));
                    assertTrue(hasLifecycleEvent(logs, "CONVERSATION_STARTED"));
                    assertTrue(hasLifecycleEvent(logs, "CONVERSATION_COMPLETED"));
                });
    }

    @Test
    public void shouldIsolateFailingCustomPublisherAfterPersistingBaseMonitoringLogs() {
        final java.util.concurrent.atomic.AtomicInteger customPublishCount =
                new java.util.concurrent.atomic.AtomicInteger();
        withDefaultFlowDependencies()
                .withBean(com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher.class,
                        () -> event -> {
                            customPublishCount.incrementAndGet();
                            throw new IllegalStateException("external exporter unavailable");
                        })
                .run(context -> {
                    AskDataResult result = context.getBean(AskDataService.class).ask(createRequest());
                    java.util.List<com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog> logs =
                            context.getBean(com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.class)
                                    .list(new com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext(
                                            "default", "default"));

                    assertTrue(result.isSuccess());
                    assertTrue(hasLifecycleEvent(logs, "CONVERSATION_STARTED"));
                    assertTrue(hasLifecycleEvent(logs, "CONVERSATION_COMPLETED"));
                    assertTrue(customPublishCount.get() > 0);
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
                        () -> request -> new AskDataMetadata("mysql",
                                Collections.singletonList(new AskDataTable("orders", Collections.emptyList())),
                                null, null, null))
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
        request.setProjectContext(new com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext("default", "default"));
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

    private boolean hasLifecycleEvent(
            java.util.List<com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog> logs,
            String eventType) {
        for (com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog log : logs) {
            if (eventType.equals(log.getEventType())) {
                return true;
            }
        }
        return false;
    }

    private ChatLanguageModel chatModel(final String response) {
        return (ChatLanguageModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {ChatLanguageModel.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("generate".equals(method.getName()) && method.getReturnType() == String.class) {
                            return response;
                        }
                        throw new UnsupportedOperationException(method.toString());
                    }
                });
    }

    private EmbeddingModel embeddingModel(final float... values) {
        return (EmbeddingModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {EmbeddingModel.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("embed".equals(method.getName())) {
                            return Response.from(Embedding.from(values));
                        }
                        throw new UnsupportedOperationException(method.toString());
                    }
                });
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
