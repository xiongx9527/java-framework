package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
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
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityRegistry;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelRegistry;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
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
            assertTrue(context.getBean(AskDataCapabilityRegistry.class) != null);
            assertTrue(context.getBean(com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepRegistry.class) != null);
        });
    }

    @Test
    public void shouldRejectAmbiguousCoreStepAgentsAtStartup() {
        contextRunner
                .withBean("firstGenerateSqlAgent", com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent.class,
                        () -> coreStepAgent("first-generator", 10))
                .withBean("secondGenerateSqlAgent", com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent.class,
                        () -> coreStepAgent("second-generator", 10))
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertTrue(failure != null);
                    String messages = failureMessages(failure);
                    assertTrue(messages.contains("generate-sql"));
                    assertTrue(messages.contains("first-generator"));
                    assertTrue(messages.contains("second-generator"));
                });
    }

    @Test
    public void shouldRegisterBusinessCoreCapabilityProvider() {
        AskDataCapabilityProvider provider = capabilityProvider("business-chat");

        contextRunner.withBean("businessChatCapability", AskDataCapabilityProvider.class, () -> provider)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertEquals("business-chat", context.getBean(AskDataCapabilityRegistry.class)
                            .get("business-chat").getKey());
                });
    }

    @Test
    public void shouldRejectDuplicateCoreCapabilityKeysAtStartup() {
        contextRunner
                .withBean("firstBusinessCapability", AskDataCapabilityProvider.class,
                        () -> capabilityProvider("duplicate-chat"))
                .withBean("secondBusinessCapability", AskDataCapabilityProvider.class,
                        () -> capabilityProvider("duplicate-chat"))
                .run(context -> {
                    assertTrue(context.getStartupFailure() != null);
                    assertTrue(context.getStartupFailure().getMessage().contains("duplicate-chat"));
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
                        new DefaultListableBeanFactory().getBeanProvider(AiEmbeddingStoreRegistry.class),
                        new DefaultListableBeanFactory().getBeanProvider(AskDataModelRegistry.class)));

        assertTrue(exception.getMessage().contains("向量"));
    }

    @Test
    public void shouldAllowSharedVectorModeWhenClusterModeIsEnabled() {
        AskDataProperties properties = new AskDataProperties();
        properties.getState().setClusterMode(true);
        properties.getVector().setStore("shared");

        AskDataVectorRetriever retriever = new AskDataAutoConfiguration().askDataVectorRetriever(properties,
                new DefaultListableBeanFactory().getBeanProvider(AiEmbeddingStoreRegistry.class),
                new DefaultListableBeanFactory().getBeanProvider(AskDataModelRegistry.class));

        assertTrue(retriever != null);
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
    public void shouldPreserveCustomAskDataVectorRetriever() {
        AskDataVectorRetriever customRetriever = query -> Collections.singletonList(new com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument("custom"));

        contextRunner.withBean(AskDataVectorRetriever.class, () -> customRetriever).run(context ->
                assertSame(customRetriever, context.getBean(AskDataVectorRetriever.class)));
    }

    @Test
    public void shouldNotUseDirectLangChain4jVectorBeansAsDefaultPath() {
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        store.add(Embedding.from(new float[] {1.0F}), TextSegment.from("legacy vector document"));

        contextRunner
                .withBean("legacyEmbeddingStore", EmbeddingStore.class, () -> store)
                .withBean("legacyEmbeddingModel", EmbeddingModel.class, () -> embeddingModel(1.0F))
                .run(context -> assertTrue(context.getBean(AskDataVectorRetriever.class).retrieve(
                        new com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery("term", "orders",
                                "legacy", "legacy", 1, Collections.<String, Object>emptyMap())).isEmpty()));
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
    public void shouldNotUseDirectLangChain4jBeansAsDefaultModelRegistryPath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("com.xiongx9527.fw.plus.ai.langchain4j.adapter"))
                .withPropertyValues(
                        "fw.plus.ai.askdata.model.default-chat-model=directChat",
                        "fw.plus.ai.askdata.model.default-embedding-model=directEmbedding")
                .withBean("directChat", ChatLanguageModel.class, () -> chatModel("direct-response"))
                .withBean("directEmbedding", EmbeddingModel.class, () -> embeddingModel(0.3F, 0.7F))
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    AskDataModelRegistry registry = context.getBean(AskDataModelRegistry.class);
                    assertThrows(com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelException.class,
                            () -> registry.getChatModel(null));
                    assertThrows(com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelException.class,
                            () -> registry.getEmbeddingModel(null));
                });
    }

    @Test
    public void shouldPreserveCustomAskDataModelRegistry() {
        AskDataModelRegistry customRegistry = new AskDataModelRegistry() {
            @Override
            public AskDataChatModel getChatModel(String modelKey) {
                return prompt -> new AskDataModelResponse("custom", null, "custom");
            }

            @Override
            public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
                return text -> java.util.Collections.singletonList(1.0F);
            }

            @Override
            public String getDefaultChatModelKey() {
                return "custom";
            }

            @Override
            public String getDefaultEmbeddingModelKey() {
                return "custom";
            }
        };

        contextRunner.withBean(AskDataModelRegistry.class, () -> customRegistry)
                .run(context -> assertSame(customRegistry, context.getBean(AskDataModelRegistry.class)));
    }

    @Test
    public void shouldUseModelsFromOptionalModelRegistry() {
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey("managed");
        definition.setFramework("langchain4j");
        definition.setProvider("test");
        definition.setType(AiModelType.CHAT);
        models.put("managed", new LangChain4jAiModel(definition,
                new LangChain4jModel("test", LangChain4jModelType.CHAT, chatModel("managed-response"))));
        AiModelRegistry modelRegistry = new DefaultAiModelRegistry(models, "managed");

        contextRunner
                .withPropertyValues("fw.plus.ai.askdata.model.default-chat-model=managed")
                .withBean(AiModelRegistry.class, () -> modelRegistry)
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
                            .loadLatest(createRequest().getProjectContext(), result.getConversationId()) != null);
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

    @Test
    public void shouldPreferBusinessCoreChatModelOverLegacyRegistry() {
        withDefaultFlowDependencies()
                .withPropertyValues("fw.plus.ai.askdata.model.default-chat-model=test-model")
                .withBean(com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel.class,
                        () -> request -> com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                                "{\"sql\":\"" + SQL + "\"}", "business-provider-chat"))
                .run(context -> {
                    AskDataRequest request = createRequest();
                    request.setModelKey("test-model");
                    AskDataResult result = context.getBean(AskDataService.class).ask(request);

                    assertTrue(result.isSuccess());
                    assertEquals("business-provider-chat", result.getActualModelKey());
                    assertEquals(Collections.singletonList("test-model"), result.getModelAttempts());
                    AskDataCheckpoint checkpoint = context.getBean(AskDataConversationStore.class)
                            .loadLatest(request.getProjectContext(), result.getConversationId());
                    assertEquals("business-provider-chat", checkpoint.getActualModelKey());
                    assertEquals(Collections.singletonList("test-model"), checkpoint.getModelAttempts());

                    java.util.List<com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog>
                            logs = context.getBean(
                                    com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.class)
                                    .list(request.getProjectContext());
                    com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog routed = null;
                    for (com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore.MonitoringLog log : logs) {
                        if ("MODEL_ROUTED".equals(log.getEventType())) {
                            routed = log;
                        }
                    }
                    assertTrue(routed != null);
                    assertEquals("test-model", routed.getModelKey());
                });
    }

    private ApplicationContextRunner withDefaultFlowDependencies() {
        return contextRunner
                .withBean(AskDataGovernanceProvider.class,
                        () -> request -> AskDataAuthorizationSummary.allow(request, Collections.singletonList(
                                new AskDataAuthorizedObject("orders", "orders", "*")),
                                Collections.emptyList(), "policy-v1", "test-governance",
                                AskDataAdministrativeScope.none()))
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

    private AskDataCapabilityProvider capabilityProvider(final String key) {
        return new AskDataCapabilityProvider() {
            @Override
            public AskDataCapabilityDescriptor getDescriptor() {
                return new AskDataCapabilityDescriptor(key, AskDataCapabilityType.MODEL, "1.0.0",
                        "0.1.1-SNAPSHOT", "0.1.1-SNAPSHOT", 0, true);
            }
        };
    }

    private com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent coreStepAgent(final String key,
            final int priority) {
        return new com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getStepKey() {
                return com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepKey.GENERATE_SQL;
            }

            @Override
            public int getPriority() {
                return priority;
            }

            @Override
            public com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepResult execute(
                    com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepContext context) {
                return com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepResult.generatedSql("select 1");
            }
        };
    }

    private String failureMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            messages.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return messages.toString();
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
