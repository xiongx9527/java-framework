package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelException;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.DefaultAskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAskDataFlowExecutorTest {

    private static final String SQL = "select count(*) from orders limit 100";

    @Test
    public void shouldExecuteDefaultFlowInOrder() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\","
                + "\"tables\":[\"orders\"],\"chartType\":\"number\",\"brief\":\"订单数\"}")
                .execute(request(true));

        assertEquals(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql", "execute-sql", "format-result"),
                stepKeys(result));
        assertEquals(SQL, result.getSql());
        assertTrue(result.isSuccess());
        assertEquals(1, dependencies.executorCalls);
        assertEquals(Arrays.asList("schema", "term", "example"), dependencies.retrieveTypes);
    }

    @Test
    public void shouldGenerateSqlThroughModelRegistryRoutingEntry() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        assertTrue(dependencies.routedGenerateCalled);
    }

    @Test
    public void shouldReturnModelNotFoundWhenConfiguredModelIsMissing() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor(null).execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_NOT_FOUND, result.getErrorCode());
    }

    @Test
    public void shouldPreserveModelRegistryErrorCode() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.modelException = new AskDataModelException(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED,
                "default model missing");

        AskDataResult result = dependencies.executor("unused").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, result.getErrorCode());
    }

    @Test
    public void shouldReturnInvalidResponseWhenModelDoesNotProduceJson() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("not json").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, result.getErrorCode());
    }

    @Test
    public void shouldRejectRequestsWithoutValidProjectOperatorScopeBeforeMainFlow() {
        StubDependencies dependencies = new StubDependencies();

        AskDataRequest missingContext = request(true);
        missingContext.setProjectContext(null);
        AskDataRequest blankProject = request(true);
        blankProject.setProjectContext(contextWithBlankProjectId());
        AskDataRequest blankOperator = request(true);
        blankOperator.setProjectContext(contextWithBlankOperatorId());

        AskDataResult missingContextResult = dependencies.executor("unused").execute(missingContext);
        AskDataResult blankProjectResult = dependencies.executor("unused").execute(blankProject);
        AskDataResult blankOperatorResult = dependencies.executor("unused").execute(blankOperator);

        assertFalse(missingContextResult.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, missingContextResult.getErrorCode());
        assertFalse(blankProjectResult.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, blankProjectResult.getErrorCode());
        assertFalse(blankOperatorResult.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, blankOperatorResult.getErrorCode());
        assertEquals(0, dependencies.metadataCalls);
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldRejectDeniedRequestBeforeModelInvocation() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request ->
                AskDataAuthorizationSummary.deny(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE);

        AskDataResult result = dependencies.executor("unused").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals(0, dependencies.metadataCalls);
    }

    @Test
    public void shouldDenyOldConstructorPathWithoutGovernanceProviderBeforeModelInvocation() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = null;

        AskDataResult result = dependencies.executor("unused").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals(0, dependencies.metadataCalls);
    }

    @Test
    public void shouldRejectEmptyAuthorizedObjectsBeforeModelInvocation() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> AskDataAuthorizationSummary.allow(Collections.emptyList(),
                Collections.emptyList());

        AskDataResult result = dependencies.executor("unused").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals(0, dependencies.metadataCalls);
    }

    @Test
    public void shouldPassAuthorizedSummaryToSqlCommand() {
        StubDependencies dependencies = new StubDependencies();
        AskDataAuthorizationSummary expected = allowedOrders();
        dependencies.governanceProvider = request -> expected;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(false));

        assertTrue(result.isSuccess());
        assertEquals(expected, dependencies.authorizationSummaryInSqlCommand);
    }

    @Test
    public void shouldRejectTableOutsideGovernanceAuthorizedObjects() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrders();
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from payments\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldDenyUnresolvedRowPredicateBeforeModelInvocation() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrdersWithRowPredicate();

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldDenyAuthorizedObjectsWithoutMetadataIntersectionBeforeSqlExecution() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> AskDataAuthorizationSummary.allow(Collections.singletonList(
                new AskDataAuthorizedObject("orders", "unknown_table", "*")), Collections.emptyList());
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from payments\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldDenyCompletedCheckpointWhenCurrentMetadataDoesNotAuthorizeCheckpointSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> AskDataAuthorizationSummary.allow(Collections.singletonList(
                new AskDataAuthorizedObject("orders", "unknown_table", "*")), Collections.emptyList());
        dependencies.conversationStore = new InMemoryAskDataConversationStore();
        dependencies.conversationStore.save(completedCheckpoint());
        AskDataRequest request = request(true);
        request.setConversationId("conversation-1");
        request.setResumeToken("resume-executed");

        AskDataResult result = dependencies.executor("unused").execute(request);

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertEquals(1, dependencies.metadataCalls);
        assertEquals(0, dependencies.executorCalls);
        assertEquals(null, result.getRowCount());
    }

    @Test
    public void shouldShortCircuitBeforeExecutorWhenSqlIsRejected() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.validationResult = AskDataSqlValidationResult.rejected(AskDataErrorCode.SQL_REJECTED, "only select allowed");

        AskDataResult result = dependencies.executor("{\"sql\":\"delete from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldReturnSqlWithoutRowsWhenExecutionIsDisabledByRequest() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(false));

        assertTrue(result.isSuccess());
        assertEquals(SQL, result.getSql());
        assertTrue(result.getRows().isEmpty());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldExecuteWhenGloballyEnabledAndRequestExecuteIsMissing() {
        StubDependencies dependencies = new StubDependencies();
        AskDataRequest request = request(true);
        request.setExecute(null);

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request);

        assertTrue(result.isSuccess());
        assertEquals(1, dependencies.executorCalls);
    }

    @Test
    public void shouldSkipWhenGloballyDisabledEvenIfRequestEnablesExecution() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.globalExecutionEnabled = false;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldContainVectorRetrieverFailureInResultWithoutExecutingSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.vectorRetriever = new AskDataVectorRetriever() {
            @Override
            public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                throw new IllegalStateException("vector unavailable");
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals("VECTOR_RETRIEVAL_FAILED", result.getErrorCode());
        assertEquals(Arrays.asList("metadata", "prompt"), stepKeys(result));
        assertFalse(result.getSteps().get(1).isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldContainDialectRegistryFailureInResultWithoutExecutingSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.dialectRegistry = new AskDataDialectRegistry() {
            @Override
            public AskDataDialect getDialect(String dialect) {
                throw new IllegalStateException("dialect unavailable");
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.DIALECT_RESOLUTION_FAILED, result.getErrorCode());
        assertEquals(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql"), stepKeys(result));
        assertFalse(result.getSteps().get(3).isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    private AskDataRequest request(boolean execute) {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单数量");
        request.setProjectContext(new AskDataProjectContext("default", "default"));
        request.setDatasourceKey("orders");
        request.setDialect("mysql");
        request.setModelKey("test-model");
        request.setExecute(Boolean.valueOf(execute));
        return request;
    }

    private AskDataProjectContext contextWithBlankProjectId() {
        return new AskDataProjectContext("project", "operator") {
            @Override
            public String getProjectId() {
                return " ";
            }
        };
    }

    private AskDataProjectContext contextWithBlankOperatorId() {
        return new AskDataProjectContext("project", "operator") {
            @Override
            public String getOperatorId() {
                return " ";
            }
        };
    }

    private static AskDataAuthorizationSummary allowedOrders() {
        return AskDataAuthorizationSummary.allow(Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "*")), Collections.emptyList());
    }

    private static AskDataAuthorizationSummary allowedOrdersWithRowPredicate() {
        AskDataAuthorizedObject orders = new AskDataAuthorizedObject("orders", "orders", "*");
        AskDataRowPredicate predicate = new AskDataRowPredicate(orders, "tenant_id = :tenantId",
                Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")));
        return AskDataAuthorizationSummary.allow(Collections.singletonList(orders), Collections.singletonList(predicate));
    }

    private static AskDataCheckpoint completedCheckpoint() {
        AskDataCheckpoint checkpoint = new AskDataCheckpoint();
        checkpoint.setConversationId("conversation-1");
        checkpoint.setCheckpointId("checkpoint-executed");
        checkpoint.setResumeToken("resume-executed");
        checkpoint.setVersion("1");
        checkpoint.setDatasourceKey("orders");
        checkpoint.setDialect("mysql");
        checkpoint.setModelKey("test-model");
        checkpoint.setRequestSummary("订单数量");
        checkpoint.setMaxRows(Integer.valueOf(100));
        checkpoint.setExecute(Boolean.TRUE);
        checkpoint.setRequestOptionsSnapshot("[\"default\",\"default\",null]");
        checkpoint.setSql(SQL);
        checkpoint.setSqlValidated(Boolean.TRUE);
        checkpoint.setSqlExecuted(Boolean.TRUE);
        checkpoint.setRowCount(Integer.valueOf(3));
        checkpoint.setCompletedSteps(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql", "execute-sql"));
        checkpoint.setCurrentStep("execute-sql");
        return checkpoint;
    }

    private List<String> stepKeys(AskDataResult result) {
        java.util.ArrayList<String> keys = new java.util.ArrayList<String>();
        for (AskDataStepLog step : result.getSteps()) {
            keys.add(step.getStepKey());
        }
        return keys;
    }

    private static class StubDependencies {

        private final AskDataProperties properties = new AskDataProperties();
        private AskDataDialectRegistry dialectRegistry = new AskDataDialectRegistry();
        private AskDataVectorRetriever vectorRetriever;
        private final List<String> retrieveTypes = new java.util.ArrayList<String>();
        private int metadataCalls;
        private int executorCalls;
        private boolean globalExecutionEnabled = true;
        private AskDataModelException modelException;
        private boolean routedGenerateCalled;
        private boolean useDefaultSqlValidator;
        private AskDataSqlValidationResult validationResult = AskDataSqlValidationResult.accepted(SQL);
        private AskDataGovernanceProvider governanceProvider = request -> allowedOrders();
        private AskDataAuthorizationSummary authorizationSummaryInSqlCommand;
        private AskDataConversationStore conversationStore;

        private DefaultAskDataFlowExecutor executor(final String modelResponse) {
            properties.getSql().setExecuteEnabled(globalExecutionEnabled);
            AskDataModelRegistry modelRegistry = new AskDataModelRegistry() {
                @Override
                public AskDataModelResponse generate(AskDataRequest request, String prompt) {
                    routedGenerateCalled = true;
                    if (modelException != null) {
                        throw modelException;
                    }
                    if (modelResponse == null) {
                        throw new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND,
                                "AskData chat model was not found");
                    }
                    return new AskDataModelResponse(modelResponse, modelResponse, "test-model");
                }

                @Override
                public AskDataChatModel getChatModel(String modelKey) {
                    if (modelException != null) {
                        throw modelException;
                    }
                    if (modelResponse == null) {
                        return null;
                    }
                    return new AskDataChatModel() {
                        @Override
                        public AskDataModelResponse generate(String prompt) {
                            return new AskDataModelResponse(modelResponse, modelResponse, "test-model");
                        }
                    };
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
            AskDataMetadataProvider metadataProvider = new AskDataMetadataProvider() {
                @Override
                public AskDataMetadata load(AskDataRequest request) {
                    metadataCalls++;
                    return new AskDataMetadata("mysql", Arrays.asList(
                            new AskDataTable("orders", Collections.emptyList()),
                            new AskDataTable("payments", Collections.emptyList())), Collections.emptyList(),
                            Collections.emptyList(), Collections.emptyList());
                }
            };
            if (vectorRetriever == null) {
                vectorRetriever = new AskDataVectorRetriever() {
                    @Override
                    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                        retrieveTypes.add(query.getRetrieveType());
                        return Collections.emptyList();
                    }
                };
            }
            AskDataPromptTemplateProvider promptProvider = new AskDataPromptTemplateProvider() {
                @Override
                public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                        List<AskDataRetrievedDocument> documents, int maxRows) {
                    return "prompt";
                }
            };
            AskDataSqlValidator sqlValidator = new AskDataSqlValidator() {
                @Override
                public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                    return validationResult;
                }
            };
            if (useDefaultSqlValidator) {
                sqlValidator = new DefaultAskDataSqlValidator();
            }
            AskDataSqlExecutor sqlExecutor = new AskDataSqlExecutor() {
                @Override
                public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
                    executorCalls++;
                    return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
                }
            };
            AskDataSqlInterceptorChain sqlInterceptorChain = new AskDataSqlInterceptorChain(Collections.emptyList(),
                    new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator() {
                        @Override
                        public Result validate(AskDataSqlCommand command) {
                            authorizationSummaryInSqlCommand = command.getAuthorizationSummary();
                            if (command.getSql().contains("payments")
                                    && !command.getAuthorizedObjects().contains("payments")) {
                                return Result.failure(FailureCategory.UNAUTHORIZED_OBJECT,
                                        "payments is not authorized");
                            }
                            return Result.success();
                        }
                    }, new AskDataSqlAudit());
            return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                    promptProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, null, null,
                    Collections.<AskDataAgent>emptyList(), null,
                    com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner.simple(), sqlInterceptorChain,
                    null, governanceProvider);
        }
    }
}
