package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn;
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
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataModelAttemptTrace;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetrievalResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptor;
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
    public void shouldUseCoreChatModelAndReturnActualModelKey() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                "{\"sql\":\"select count(*) from orders\"}", "provider-chat");

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertTrue(result.isSuccess());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals("provider-chat", result.getActualModelKey());
        assertEquals(Collections.singletonList("test-model"), result.getModelAttempts());
    }

    @Test
    public void shouldUseDedicatedCoreModelRequestTimeout() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.properties.getModel().setRequestTimeoutMillis(1800);
        dependencies.properties.getModel().setInfrastructureFailureTimeoutMillis(1);
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> {
            assertEquals(1800L, request.getTimeoutMillis());
            return com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                    "{\"sql\":\"select count(*) from orders\"}", "provider-chat");
        };

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertTrue(result.isSuccess());
    }

    @Test
    public void shouldFallbackOnlyAfterRetryableCoreModelFailure() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.properties.getModel().setChatCandidates(Collections.singletonList("fallback-model"));
        dependencies.properties.getModel().setDefaultChatModel("fallback-model");
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> "test-model".equals(request.getModelKey())
                ? com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.failure("test-model",
                        "MODEL_INFRASTRUCTURE_FAILURE", "temporary outage", true)
                : com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                        "{\"sql\":\"select count(*) from orders\"}", "provider-fallback");

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertTrue(result.isSuccess());
        assertEquals(Arrays.asList("test-model", "fallback-model"), result.getModelAttempts());
        assertEquals("provider-fallback", result.getActualModelKey());
    }

    @Test
    public void shouldPublishStructuredEventsForEveryCoreModelAttempt() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.properties.getModel().setChatCandidates(Collections.singletonList("fallback-model"));
        dependencies.properties.getModel().setDefaultChatModel("fallback-model");
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> "test-model".equals(request.getModelKey())
                ? com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.failure("test-model",
                        "MODEL_INFRASTRUCTURE_FAILURE", "temporary outage", true)
                : com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                        "{\"sql\":\"select count(*) from orders\"}", "provider-fallback");

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertTrue(result.isSuccess());
        List<AskDataEvent> modelEvents = new java.util.ArrayList<AskDataEvent>();
        for (AskDataEvent event : dependencies.events) {
            if (event.getType() == AskDataEvent.Type.MODEL_ROUTED) {
                modelEvents.add(event);
            }
        }
        assertEquals(4, modelEvents.size());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, modelEvents.get(0).getModelState());
        assertEquals(com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy.REQUEST_MODEL_KEY,
                modelEvents.get(0).getModelRoutePolicy());
        assertEquals(AskDataEvent.ModelState.FAILED, modelEvents.get(1).getModelState());
        assertEquals("MODEL_INFRASTRUCTURE_FAILURE", modelEvents.get(1).getModelFailureCode());
        assertEquals("RETRYABLE_INFRASTRUCTURE_FAILURE", modelEvents.get(1).getModelSwitchReason());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, modelEvents.get(2).getModelState());
        assertEquals(AskDataEvent.ModelState.SUCCEEDED, modelEvents.get(3).getModelState());
        assertEquals("provider-fallback", modelEvents.get(3).getActualModelKey());
    }

    @Test
    public void shouldRecordEveryEmbeddingAttemptInResultCheckpointAndEvents() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.conversationStore = new InMemoryAskDataConversationStore();
        dependencies.contextCompressor = new DefaultAskDataContextCompressor(dependencies.properties);
        dependencies.vectorRetriever = new AskDataVectorRetriever() {
            @Override
            public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                return retrieveWithResult(query).getDocuments();
            }

            @Override
            public AskDataVectorRetrievalResult retrieveWithResult(AskDataVectorQuery query) {
                dependencies.retrieveTypes.add(query.getRetrieveType());
                dependencies.vectorQueries.add(query);
                return new AskDataVectorRetrievalResult(Collections.<AskDataRetrievedDocument>emptyList(),
                        Arrays.asList(
                                AskDataModelAttemptTrace.attempted("embedding-primary",
                                        com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy.PROJECT_KEY),
                                AskDataModelAttemptTrace.failed("embedding-primary",
                                        com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy.PROJECT_KEY,
                                        "MODEL_INFRASTRUCTURE_FAILURE",
                                        "RETRYABLE_INFRASTRUCTURE_FAILURE", "embedding-primary"),
                                AskDataModelAttemptTrace.attempted("embedding-fallback",
                                        com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy.DEFAULT_KEY),
                                AskDataModelAttemptTrace.succeeded("embedding-fallback",
                                        com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy.DEFAULT_KEY,
                                        "provider-embedding-fallback")), "provider-embedding-fallback");
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        assertEquals("provider-embedding-fallback", result.getActualEmbeddingModelKey());
        assertEquals(Arrays.asList("embedding-primary", "embedding-fallback", "embedding-primary",
                "embedding-fallback", "embedding-primary", "embedding-fallback"),
                result.getEmbeddingModelAttempts());
        AskDataCheckpoint checkpoint = dependencies.conversationStore.loadLatest(result.getConversationId());
        assertEquals("provider-embedding-fallback", checkpoint.getActualEmbeddingModelKey());
        assertEquals(result.getEmbeddingModelAttempts(), checkpoint.getEmbeddingModelAttempts());
        List<AskDataEvent> embeddingEvents = new java.util.ArrayList<AskDataEvent>();
        for (AskDataEvent event : dependencies.events) {
            if (event.getType() == AskDataEvent.Type.MODEL_ROUTED
                    && event.getModelType() == AskDataEvent.ModelType.EMBEDDING) {
                embeddingEvents.add(event);
            }
        }
        assertEquals(12, embeddingEvents.size());
        assertEquals(AskDataEvent.ModelState.ATTEMPTED, embeddingEvents.get(0).getModelState());
        assertEquals(AskDataEvent.ModelState.FAILED, embeddingEvents.get(1).getModelState());
        assertEquals("RETRYABLE_INFRASTRUCTURE_FAILURE", embeddingEvents.get(1).getModelSwitchReason());
        assertEquals(AskDataEvent.ModelState.SUCCEEDED, embeddingEvents.get(3).getModelState());
        assertEquals("provider-embedding-fallback", embeddingEvents.get(3).getActualModelKey());
    }

    @Test
    public void shouldNotFallbackAfterNonRetryableCoreModelFailure() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.properties.getModel().setChatCandidates(Collections.singletonList("fallback-model"));
        dependencies.properties.getModel().setDefaultChatModel("fallback-model");
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.failure(
                request.getModelKey(), "MODEL_REQUEST_REJECTED", "invalid credentials", false);

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals("MODEL_REQUEST_REJECTED", result.getErrorCode());
        assertEquals(Collections.singletonList("test-model"), result.getModelAttempts());
    }

    @Test
    public void shouldNotFallbackWhenNonInfrastructureFailureClaimsRetryable() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.properties.getModel().setChatCandidates(Collections.singletonList("fallback-model"));
        dependencies.properties.getModel().setDefaultChatModel("fallback-model");
        dependencies.coreModelRouter = new com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter() { };
        dependencies.coreChatModel = request -> "test-model".equals(request.getModelKey())
                ? com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.failure("test-model",
                        "MODEL_REQUEST_REJECTED", "invalid request", true)
                : com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult.success(
                        "{\"sql\":\"select count(*) from orders\"}", "must-not-run");

        AskDataResult result = dependencies.executor("legacy-must-not-run").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals("MODEL_REQUEST_REJECTED", result.getErrorCode());
        assertEquals(Collections.singletonList("test-model"), result.getModelAttempts());
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
                AskDataAuthorizationSummary.deny(request, DefaultDenyAskDataGovernanceProvider.DENIAL_CODE,
                        "policy-v1", "test-governance");

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
        dependencies.governanceProvider = request -> allowed(request,
                Collections.<AskDataAuthorizedObject>emptyList(), Collections.<AskDataRowPredicate>emptyList());

        AskDataResult result = dependencies.executor("unused").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertFalse(dependencies.routedGenerateCalled);
        assertEquals(0, dependencies.metadataCalls);
    }

    @Test
    public void shouldFilterUnauthorizedColumnsBeforeBuildingPrompt() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "id")), Collections.<AskDataRowPredicate>emptyList());

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(false));

        assertTrue(result.isSuccess());
        assertTrue(dependencies.prompt.contains("id bigint"));
        assertFalse(dependencies.prompt.contains("secret varchar"));
    }

    @Test
    public void shouldPassGovernanceScopeToEveryVectorQuery() {
        StubDependencies dependencies = new StubDependencies();

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(false));

        assertTrue(result.isSuccess());
        assertEquals(3, dependencies.vectorQueries.size());
        for (AskDataVectorQuery query : dependencies.vectorQueries) {
            assertEquals("default", query.getProjectContext().getProjectId());
            assertEquals("default", query.getProjectContext().getOperatorId());
            assertEquals("orders", query.getFilters().get("datasourceKey"));
            assertEquals("policy-v1", query.getFilters().get("authorizationVersion"));
        }
    }

    @Test
    public void shouldRejectModelGeneratedUnauthorizedColumn() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "id")), Collections.<AskDataRowPredicate>emptyList());
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select secret from orders\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldPublishGovernanceEventWhenSqlAuthorizationFails() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "id")), Collections.<AskDataRowPredicate>emptyList());
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select secret from orders\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        AskDataEvent event = null;
        for (AskDataEvent candidate : dependencies.events) {
            if (candidate.getType() == AskDataEvent.Type.GOVERNANCE_DENIED) {
                event = candidate;
            }
        }
        assertTrue(event != null);
        assertEquals("orders", event.getDatasourceKey());
        assertEquals(AskDataErrorCode.SQL_REJECTED, event.getDenialCode());
        assertEquals("policy-v1", event.getAuthorizationVersion());
        assertEquals("test-governance", event.getAuthorizationSource());
        assertFalse(event.getMessage().contains("secret"));
    }

    @Test
    public void shouldRejectPluginRewriteToUnauthorizedColumn() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "id")), Collections.<AskDataRowPredicate>emptyList());
        dependencies.useDefaultSqlValidator = true;
        dependencies.sqlInterceptors = Collections.singletonList(new AskDataSqlInterceptor() {
            @Override public String getKey() { return "rewrite-secret"; }
            @Override public int getOrder() { return 1; }
            @Override public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                return command.withSql("select secret from orders");
            }
        });

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldPassAuthorizedSummaryToSqlCommand() {
        StubDependencies dependencies = new StubDependencies();
        final AskDataAuthorizationSummary[] expected = new AskDataAuthorizationSummary[1];
        dependencies.governanceProvider = request -> expected[0] = allowedOrders(request);

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}")
                .execute(request(false));

        assertTrue(result.isSuccess());
        assertEquals(expected[0], dependencies.authorizationSummaryInSqlCommand);
    }

    @Test
    public void shouldRejectTableOutsideGovernanceAuthorizedObjects() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrders(request);
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from payments\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldInjectRowPredicateAndPassGovernedParameterToFinalCommand() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrdersWithRowPredicate(request);
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        assertTrue(dependencies.executedCommand.getSql().toLowerCase().contains("tenant_id = :tenantid"));
        assertEquals("tenant-a", dependencies.executedCommand.getParameter("tenantId").getValue());
    }

    @Test
    public void shouldMergeRowPredicateWithExistingWhereClause() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrdersWithRowPredicate(request);
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders where id > 10\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        String executedSql = dependencies.executedCommand.getSql().toLowerCase();
        assertTrue(executedSql.contains("id > 10"));
        assertTrue(executedSql.contains("tenant_id = :tenantid"));
        assertTrue(executedSql.contains(" and "));
    }

    @Test
    public void shouldReapplyRequiredRowPredicateAfterPluginDeletesWhereClause() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrdersWithRowPredicate(request);
        dependencies.useDefaultSqlValidator = true;
        dependencies.sqlInterceptors = Collections.singletonList(new AskDataSqlInterceptor() {
            @Override public String getKey() { return "delete-row-predicate"; }
            @Override public int getOrder() { return 1; }
            @Override public AskDataSqlCommand intercept(AskDataSqlCommand command) {
                assertTrue(command.getSql().toLowerCase().contains("tenant_id = :tenantid"));
                return command.withSql("select id from orders");
            }
        });

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(true));

        assertTrue(result.isSuccess());
        assertTrue(dependencies.executedCommand.getSql().toLowerCase().contains("tenant_id = :tenantid"));
        assertEquals("tenant-a", dependencies.executedCommand.getParameter("tenantId").getValue());
    }

    @Test
    public void shouldRejectRequestAndPredicateParameterNameConflict() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowedOrdersWithRowPredicate(request);
        AskDataRequest request = request(true);
        request.setOptions(Collections.<String, Object>singletonMap("tenantId", "tenant-b"));

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}").execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldDenyAuthorizedObjectsWithoutMetadataIntersectionBeforeSqlExecution() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "unknown_table", "*")),
                Collections.<AskDataRowPredicate>emptyList());
        dependencies.useDefaultSqlValidator = true;

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from payments\"}")
                .execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldSanitizeCustomMetadataProviderOutputInsideFlow() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "id")),
                Collections.<AskDataRowPredicate>emptyList());
        dependencies.metadataProvider = new AskDataMetadataProvider() {
            @Override
            public AskDataMetadata load(AskDataRequest request) {
                throw new AssertionError("Flow should use authorization-aware metadata loading");
            }

            @Override
            public AskDataMetadata load(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary) {
                return new AskDataMetadata("mysql", Arrays.asList(
                        new AskDataTable("orders", Arrays.asList(new AskDataColumn("id", "bigint"),
                                new AskDataColumn("secret", "varchar"))),
                        new AskDataTable("payments", Collections.singletonList(
                                new AskDataColumn("card_number", "varchar")))),
                        Collections.singletonList("secret business term"),
                        Collections.singletonList("select secret from orders"),
                        Collections.singletonList("always expose payments.card_number"));
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(false));

        assertTrue(result.isSuccess());
        assertTrue(dependencies.prompt.contains("orders.id"));
        assertFalse(dependencies.prompt.contains("secret"));
        assertFalse(dependencies.prompt.contains("payments"));
        assertFalse(dependencies.prompt.contains("card_number"));
        assertFalse(dependencies.prompt.contains("business term"));
        assertFalse(dependencies.prompt.contains("always expose"));
    }

    @Test
    public void shouldPublishSanitizedGovernanceEventForDefaultDenial() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = new DefaultDenyAskDataGovernanceProvider();
        AskDataRequest request = request(false);
        request.setOptions(Collections.<String, Object>singletonMap("credential", "never-log-this"));

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}").execute(request);

        assertFalse(result.isSuccess());
        assertEquals(1, dependencies.events.size());
        AskDataEvent event = dependencies.events.get(0);
        assertEquals(AskDataEvent.Type.GOVERNANCE_DENIED, event.getType());
        assertEquals("orders", event.getDatasourceKey());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, event.getDenialCode());
        assertEquals(DefaultDenyAskDataGovernanceProvider.AUTHORIZATION_VERSION,
                event.getAuthorizationVersion());
        assertEquals(DefaultDenyAskDataGovernanceProvider.AUTHORIZATION_SOURCE, event.getAuthorizationSource());
        assertFalse(event.getTraceId() == null || event.getTraceId().trim().isEmpty());
        assertFalse(event.getMessage().contains("never-log-this"));
    }

    @Test
    public void shouldPublishSanitizedGovernanceEventWhenProviderFails() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> {
            throw new IllegalStateException("credential=never-log-this");
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select id from orders\"}")
                .execute(request(false));

        assertFalse(result.isSuccess());
        assertEquals(1, dependencies.events.size());
        AskDataEvent event = dependencies.events.get(0);
        assertEquals(AskDataEvent.Type.GOVERNANCE_DENIED, event.getType());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, event.getDenialCode());
        assertFalse(event.getAuthorizationVersion() == null || event.getAuthorizationVersion().trim().isEmpty());
        assertFalse(event.getAuthorizationSource() == null || event.getAuthorizationSource().trim().isEmpty());
        assertFalse(event.getMessage().contains("never-log-this"));
        assertFalse(event.getAuthorizationSource().contains("never-log-this"));
    }

    @Test
    public void shouldDenyCompletedCheckpointWhenCurrentMetadataDoesNotAuthorizeCheckpointSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.governanceProvider = request -> allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "unknown_table", "*")),
                Collections.<AskDataRowPredicate>emptyList());
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

    private static AskDataAuthorizationSummary allowedOrders(AskDataAuthorizationRequest request) {
        return allowed(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "*")),
                Collections.<AskDataRowPredicate>emptyList());
    }

    private static AskDataAuthorizationSummary allowedOrdersWithRowPredicate(AskDataAuthorizationRequest request) {
        AskDataAuthorizedObject orders = new AskDataAuthorizedObject("orders", "orders", "*");
        AskDataRowPredicate predicate = new AskDataRowPredicate(orders, "tenant_id = :tenantId",
                Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")));
        return allowed(request, Collections.singletonList(orders), Collections.singletonList(predicate));
    }

    private static AskDataAuthorizationSummary allowed(AskDataAuthorizationRequest request,
            List<AskDataAuthorizedObject> objects, List<AskDataRowPredicate> predicates) {
        return AskDataAuthorizationSummary.allow(request, objects, predicates, "policy-v1", "test-governance",
                AskDataAdministrativeScope.none());
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
        private final List<AskDataVectorQuery> vectorQueries = new java.util.ArrayList<AskDataVectorQuery>();
        private int metadataCalls;
        private int executorCalls;
        private boolean globalExecutionEnabled = true;
        private AskDataModelException modelException;
        private boolean routedGenerateCalled;
        private String prompt;
        private boolean useDefaultSqlValidator;
        private AskDataSqlValidationResult validationResult = AskDataSqlValidationResult.accepted(SQL);
        private AskDataGovernanceProvider governanceProvider = request -> allowedOrders(request);
        private AskDataMetadataProvider metadataProvider;
        private AskDataAuthorizationSummary authorizationSummaryInSqlCommand;
        private AskDataSqlCommand executedCommand;
        private List<AskDataSqlInterceptor> sqlInterceptors = Collections.emptyList();
        private AskDataConversationStore conversationStore;
        private AskDataContextCompressor contextCompressor;
        private final List<AskDataEvent> events = new java.util.ArrayList<AskDataEvent>();
        private com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel;
        private com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter coreModelRouter;

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
            if (metadataProvider == null) {
                metadataProvider = new AskDataMetadataProvider() {
                    @Override
                    public AskDataMetadata load(AskDataRequest request) {
                        metadataCalls++;
                        return new AskDataMetadata("mysql", Arrays.asList(
                                new AskDataTable("orders", Arrays.asList(new AskDataColumn("id", "bigint"),
                                        new AskDataColumn("secret", "varchar"),
                                        new AskDataColumn("tenant_id", "varchar"))),
                                new AskDataTable("payments", Collections.emptyList())), Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList());
                    }
                };
            }
            if (vectorRetriever == null) {
                vectorRetriever = new AskDataVectorRetriever() {
                    @Override
                    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                        retrieveTypes.add(query.getRetrieveType());
                        vectorQueries.add(query);
                        return Collections.emptyList();
                    }
                };
            }
            AskDataPromptTemplateProvider promptProvider = new AskDataPromptTemplateProvider() {
                @Override
                public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                        List<AskDataRetrievedDocument> documents, int maxRows) {
                    StringBuilder observedMetadata = new StringBuilder();
                    for (AskDataTable table : metadata.getTables()) {
                        for (AskDataColumn column : table.getColumns()) {
                            observedMetadata.append(table.getName()).append(".").append(column.getName()).append(" ")
                                    .append(column.getType()).append("\n");
                        }
                    }
                    observedMetadata.append(metadata.getTerms()).append(metadata.getExamples())
                            .append(metadata.getPromptFragments());
                    prompt = observedMetadata.toString();
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

                @Override
                public AskDataSqlExecutionResult execute(AskDataRequest request, AskDataSqlCommand command) {
                    executedCommand = command;
                    return execute(request, command.getSql());
                }
            };
            AskDataSqlInterceptorChain sqlInterceptorChain = new AskDataSqlInterceptorChain(sqlInterceptors,
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
                    promptProvider, dialectRegistry, sqlValidator, sqlExecutor, conversationStore, contextCompressor, null,
                    Collections.<AskDataAgent>emptyList(), event -> events.add(event),
                    com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner.simple(), sqlInterceptorChain,
                    null, governanceProvider, coreChatModel, coreModelRouter);
        }
    }
}
