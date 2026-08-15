package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataMemoryRecallAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataRecommendationAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataMultiQuestionFlowTest {

    private static final String FIRST_QUESTION = "订单数量";
    private static final String SECOND_QUESTION = "退款数量";

    @Test
    public void shouldCreateAuditedChildResultsForExplicitTwoQuestionRequest() {
        assertAuditedChildResults(executor(false).execute(request(FIRST_QUESTION, SECOND_QUESTION)));
    }

    @Test
    public void shouldCreateAuditedChildResultsForCompoundQuestionPlan() {
        assertAuditedChildResults(executor(false).execute(compoundRequest()));
    }

    private void assertAuditedChildResults(AskDataResult result) {
        List<AskDataResult> children = result.getChildResults();
        assertEquals(2, children.size());
        assertTrue(children.get(0).isSuccess(), children.get(0).getErrorCode() + ":"
                + children.get(0).getErrorMessage());
        assertTrue(children.get(1).isSuccess(), children.get(1).getErrorCode() + ":"
                + children.get(1).getErrorMessage());
        assertTrue(hasChildAuditStep(children.get(0)));
        assertTrue(hasChildAuditStep(children.get(1)));
    }

    @Test
    public void shouldKeepSuccessfulChildWhenAnotherPlannedQuestionFails() {
        AskDataResult result = executor(true).execute(compoundRequest());

        List<AskDataResult> children = result.getChildResults();
        assertFalse(result.isSuccess());
        assertEquals(2, children.size());
        assertTrue(children.get(0).isSuccess());
        assertFalse(children.get(1).isSuccess());
    }

    @Test
    public void shouldRecallOnlyEntriesForCurrentProjectOperator() {
        AskDataLongTermMemory memory = AskDataLongTermMemory.inMemory();
        AskDataProjectContext current = new AskDataProjectContext("project-a", "operator-a");
        memory.remember(new AskDataMemoryEntry(current, "orders", "v1", "current operator"));
        memory.remember(new AskDataMemoryEntry(new AskDataProjectContext("project-a", "operator-b"), "orders", "v1",
                "another operator"));

        List<AskDataMemoryEntry> recalled = new AskDataMemoryRecallAdapter(memory).recall(current, "orders", "v1");

        assertEquals(1, recalled.size());
        assertEquals("current operator", recalled.get(0).getContent());
    }

    @Test
    public void shouldKeepDefaultRecommendationsWithinProjectOperatorScope() {
        AskDataRecommendationService recommendations = AskDataRecommendationService.inMemory();
        AskDataProjectContext current = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext another = new AskDataProjectContext("project-a", "operator-b");

        AskDataRecommendationAdapter adapter = new AskDataRecommendationAdapter(recommendations);
        adapter.record(current, "current operator");
        adapter.record(another, "another operator");

        assertEquals(Collections.singletonList("current operator"), adapter.recommend(current));
    }

    @Test
    public void shouldPersistParentRuntimeStateAndUseIndependentChildIdempotencyKeys() {
        com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores stores =
                com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores.singleNode();
        AskDataRuntimeStateServices stateServices = new AskDataRuntimeStateServices(stores.agentStates(),
                stores.leases(), stores.idempotencyKeys(), stores.auditStates(), AskDataLongTermMemory.inMemory());
        List<com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent> events =
                new java.util.ArrayList<com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent>();
        AskDataRequest request = request(FIRST_QUESTION, SECOND_QUESTION);
        request.getOptions().put("idempotencyKey", "batch-1");

        AskDataResult result = executor(false, new AskDataProperties(), null, null, null, stateServices,
                events::add).execute(request);

        AskDataProjectContext context = request.getProjectContext();
        assertTrue(result.isSuccess());
        assertNotNull(stores.idempotencyKeys().load(context, "batch-1"));
        assertNotNull(stores.idempotencyKeys().load(context, "batch-1:question-1"));
        assertNotNull(stores.idempotencyKeys().load(context, "batch-1:question-2"));
        assertEquals("SUCCEEDED", stores.agentStates().load(context, result.getConversationId()).getStatus());
        assertEquals(2, stores.agentStates().load(context, result.getConversationId()).getSubTasks().size());
        assertEquals("SKIP", stores.agentStates().load(context, result.getConversationId())
                .getSubTasks().get(0).getFailurePolicy());
        assertEquals("SUCCEEDED", stores.auditStates().load(context, result.getConversationId()).getStatus());
        assertTrue(stores.leases().tryAcquire(context, result.getConversationId(), "next-owner", 1000L));
        assertParentCorrelation(events, result.getConversationId(),
                (String) request.getOptions().get("askDataObservabilityRequestId"));
    }

    @Test
    public void shouldPersistFailedParentTerminalState() {
        com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores stores =
                com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores.singleNode();
        AskDataRuntimeStateServices stateServices = new AskDataRuntimeStateServices(stores.agentStates(),
                stores.leases(), stores.idempotencyKeys(), stores.auditStates(), AskDataLongTermMemory.inMemory());

        AskDataResult result = executor(true, new AskDataProperties(), null, null, null, stateServices, null)
                .execute(compoundRequest());

        AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
        assertFalse(result.isSuccess());
        assertEquals("FAILED", stores.agentStates().load(context, result.getConversationId()).getStatus());
        assertEquals("FAILED", stores.auditStates().load(context, result.getConversationId()).getStatus());
        assertTrue(stores.leases().tryAcquire(context, result.getConversationId(), "next-owner", 1000L));
    }

    @Test
    public void shouldKeepSingleQuestionFlowWhenPlannerReturnsSingleQuestion() {
        AskDataRequest request = baseRequest(FIRST_QUESTION);

        AskDataResult result = executor(false).execute(request);

        assertTrue(result.isSuccess(), result.getErrorCode() + ":" + result.getErrorMessage());
        assertTrue(result.getChildResults().isEmpty());
    }

    @Test
    public void shouldRejectMultiQuestionRequestWithResumeTokenBeforeCreatingChildren() {
        AskDataRequest request = request(FIRST_QUESTION, SECOND_QUESTION);
        request.setResumeToken("resume-token");

        AskDataResult result = executor(false).execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertTrue(result.getChildResults().isEmpty());
    }

    @Test
    public void shouldExecuteStructuredDagWithStableIdsWithinConcurrencyLimit() {
        AskDataProperties properties = new AskDataProperties();
        properties.getFlow().setMultiQuestionMaxConcurrency(2);
        properties.getFlow().setMultiQuestionTimeoutMillis(2000);
        CountDownLatch independentStarted = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        QuestionExecutionProbe probe = question -> {
            int current = active.incrementAndGet();
            updateMax(maxActive, current);
            try {
                if (!"third".equals(question)) {
                    independentStarted.countDown();
                    assertTrue(independentStarted.await(1, TimeUnit.SECONDS));
                }
            } finally {
                active.decrementAndGet();
            }
        };

        AskDataResult result = executor(false, properties, probe).execute(structuredDagRequest(0L));

        assertTrue(result.isSuccess());
        assertEquals(2, maxActive.get());
        assertEquals(Arrays.asList("q1", "q2", "q3"), childQuestionIds(result));
        assertEquals("SUCCEEDED", result.getChildResults().get(0).getQuestionState());
        assertEquals("SUCCEEDED", result.getChildResults().get(1).getQuestionState());
        assertEquals("SUCCEEDED", result.getChildResults().get(2).getQuestionState());
    }

    @Test
    public void shouldKeepTimedOutAndSkippedChildrenAsIndependentTerminalResults() {
        AskDataProperties properties = new AskDataProperties();
        properties.getFlow().setMultiQuestionMaxConcurrency(1);
        properties.getFlow().setMultiQuestionTimeoutMillis(1000);
        QuestionExecutionProbe probe = question -> {
            if ("first".equals(question)) {
                Thread.sleep(500L);
            }
        };

        AskDataResult result = executor(false, properties, probe).execute(structuredDagRequest(30L));

        assertFalse(result.isSuccess());
        assertEquals("TIMED_OUT", result.getChildResults().get(0).getQuestionState());
        assertEquals(AskDataErrorCode.SUBQUESTION_TIMED_OUT,
                result.getChildResults().get(0).getErrorCode());
        assertEquals("SUCCEEDED", result.getChildResults().get(1).getQuestionState());
        assertEquals("SKIPPED", result.getChildResults().get(2).getQuestionState());
        assertEquals(AskDataErrorCode.SUBQUESTION_SKIPPED,
                result.getChildResults().get(2).getErrorCode());
    }

    @Test
    public void shouldResumePendingDagNodesWithoutReplayingCompletedSql() {
        AskDataProperties properties = new AskDataProperties();
        properties.getSql().setExecuteEnabled(true);
        properties.getFlow().setMultiQuestionMaxConcurrency(1);
        InMemoryAskDataConversationStore seedStore = new InMemoryAskDataConversationStore();
        AskDataRequest seedRequest = structuredDagRequest(0L);
        seedRequest.setExecute(Boolean.TRUE);
        AskDataResult seedResult = executor(false, properties, null, seedStore,
                new DefaultAskDataContextCompressor(properties)).execute(seedRequest);
        assertTrue(seedResult.isSuccess());
        AskDataCheckpoint checkpoint = seedStore.loadLatest(seedRequest.getProjectContext(),
                seedResult.getConversationId());
        assertNotNull(checkpoint);
        AskDataResult completed = new AskDataResult();
        completed.setSuccess(true);
        completed.setQuestionId("q1");
        completed.setQuestionState("SUCCEEDED");
        completed.setConversationId(checkpoint.getConversationId() + ":q1");
        completed.setSql("select count(*) from orders limit 100");
        completed.setRowCount(Integer.valueOf(0));
        completed.setBrief("restored");
        checkpoint.setChildResults(Collections.singletonList(completed));
        checkpoint.setResumeToken("resume-parent");
        InMemoryAskDataConversationStore store = new InMemoryAskDataConversationStore();
        store.save(checkpoint);
        AtomicInteger firstModelCalls = new AtomicInteger();
        AtomicInteger firstSqlCalls = new AtomicInteger();
        AtomicInteger totalSqlCalls = new AtomicInteger();
        QuestionExecutionProbe probe = new QuestionExecutionProbe() {
            @Override
            public void beforeModel(String question) {
                if ("first".equals(question)) {
                    firstModelCalls.incrementAndGet();
                }
            }

            @Override
            public void beforeSql(String question) {
                totalSqlCalls.incrementAndGet();
                if ("first".equals(question)) {
                    firstSqlCalls.incrementAndGet();
                }
            }
        };
        AskDataRequest request = structuredDagRequest(0L);
        request.setExecute(Boolean.TRUE);
        request.setResumeToken("resume-parent");

        AskDataResult result = executor(false, properties, probe, store,
                new DefaultAskDataContextCompressor(properties)).execute(request);

        assertTrue(result.isSuccess(), result.getErrorCode() + ":" + result.getErrorMessage());
        assertEquals(0, firstModelCalls.get());
        assertEquals(0, firstSqlCalls.get());
        assertEquals(2, totalSqlCalls.get());
        assertEquals(Arrays.asList("q1", "q2", "q3"), childQuestionIds(result));
        assertEquals("restored", result.getChildResults().get(0).getBrief());
    }

    private AskDataRequest request(String... questions) {
        AskDataRequest request = baseRequest(questions[0]);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("questions", Arrays.asList(questions));
        request.setOptions(options);
        return request;
    }

    private AskDataRequest compoundRequest() {
        return baseRequest(FIRST_QUESTION + "；" + SECOND_QUESTION);
    }

    private AskDataRequest structuredDagRequest(long firstTimeoutMillis) {
        AskDataRequest request = baseRequest("structured plan");
        Map<String, Object> first = question("q1", "first", Collections.<String>emptyList());
        if (firstTimeoutMillis > 0L) {
            first.put("timeoutMillis", Long.valueOf(firstTimeoutMillis));
        }
        Map<String, Object> second = question("q2", "second", Collections.<String>emptyList());
        Map<String, Object> third = question("q3", "third", Arrays.asList("q1", "q2"));
        request.setOptions(Collections.<String, Object>singletonMap("questions", Arrays.asList(first, second, third)));
        return request;
    }

    private Map<String, Object> question(String id, String question, List<String> dependsOn) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("id", id);
        value.put("question", question);
        value.put("dependsOn", dependsOn);
        return value;
    }

    private List<String> childQuestionIds(AskDataResult result) {
        List<String> ids = new java.util.ArrayList<String>();
        for (AskDataResult child : result.getChildResults()) {
            ids.add(child.getQuestionId());
        }
        return ids;
    }

    private AskDataRequest baseRequest(String question) {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion(question);
        request.setProjectContext(new AskDataProjectContext("project-a", "operator-a"));
        request.setDatasourceKey("orders");
        request.setDialect("mysql");
        request.setModelKey("test-model");
        request.setExecute(Boolean.FALSE);
        return request;
    }

    private DefaultAskDataFlowExecutor executor(final boolean failSecondQuestion) {
        return executor(failSecondQuestion, new AskDataProperties(), null);
    }

    private DefaultAskDataFlowExecutor executor(final boolean failSecondQuestion, AskDataProperties properties,
            final QuestionExecutionProbe probe) {
        return executor(failSecondQuestion, properties, probe, null, null);
    }

    private DefaultAskDataFlowExecutor executor(final boolean failSecondQuestion, AskDataProperties properties,
            final QuestionExecutionProbe probe, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor) {
        return executor(failSecondQuestion, properties, probe, conversationStore, contextCompressor, null, null);
    }

    private DefaultAskDataFlowExecutor executor(final boolean failSecondQuestion, AskDataProperties properties,
            final QuestionExecutionProbe probe, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataRuntimeStateServices runtimeStateServices,
            com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher eventPublisher) {
        AskDataModelRegistry modelRegistry = new AskDataModelRegistry() {
            @Override
            public AskDataModelResponse generate(AskDataRequest request, String prompt) {
                if (probe != null) {
                    try {
                        probe.beforeModel(request.getQuestion());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("question execution interrupted", ex);
                    }
                }
                if (failSecondQuestion && SECOND_QUESTION.equals(request.getQuestion())) {
                    return new AskDataModelResponse("not-json", "not-json", "test-model");
                }
                return new AskDataModelResponse("{\"sql\":\"select count(*) from orders\",\"brief\":\"ok\"}",
                        "response", "test-model");
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
        AskDataMetadataProvider metadataProvider = new AskDataMetadataProvider() {
            @Override
            public AskDataMetadata load(AskDataRequest request) {
                return new AskDataMetadata("mysql", Collections.singletonList(
                        new AskDataTable("orders", Collections.emptyList())), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList());
            }
        };
        AskDataVectorRetriever vectorRetriever = new AskDataVectorRetriever() {
            @Override
            public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                return Collections.emptyList();
            }
        };
        AskDataPromptTemplateProvider promptProvider = new AskDataPromptTemplateProvider() {
            @Override
            public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                    List<AskDataRetrievedDocument> documents, int maxRows) {
                return "prompt";
            }
        };
        AskDataSqlValidator validator = new AskDataSqlValidator() {
            @Override
            public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                return AskDataSqlValidationResult.accepted(sql + " limit " + maxRows);
            }
        };
        AskDataSqlExecutor sqlExecutor = new AskDataSqlExecutor() {
            @Override
            public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
                if (probe != null) {
                    probe.beforeSql(request.getQuestion());
                }
                return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
            }
        };
        return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                promptProvider, new AskDataDialectRegistry(), validator, sqlExecutor, conversationStore,
                contextCompressor, null,
                Collections.<AskDataAgent>emptyList(), eventPublisher,
                com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner.simple(), null, null,
                request -> allowedOrders(request), null, null, null, runtimeStateServices);
    }

    private void assertParentCorrelation(
            List<com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent> events, String conversationId,
            String requestId) {
        String rootEventId = requestId + ":root";
        boolean rootFound = false;
        java.util.Set<String> childRoots = new java.util.HashSet<String>();
        java.util.Set<String> childIds = new java.util.HashSet<String>();
        for (com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent event : events) {
            if (event.getCorrelation() == null) {
                continue;
            }
            assertEquals(requestId, event.getCorrelation().getRequestId());
            if (event.getType()
                    == com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent.Type.CONVERSATION_STARTED
                    && conversationId.equals(event.getCorrelation().getConversationId())) {
                rootFound = rootEventId.equals(event.getCorrelation().getEventId())
                        && event.getCorrelation().getSubQuestionId() == null;
            } else if (event.getType()
                    == com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent.Type.CONVERSATION_STARTED
                    && event.getCorrelation().getSubQuestionId() != null) {
                String childId = event.getCorrelation().getSubQuestionId();
                assertEquals(conversationId + ":" + childId, event.getCorrelation().getConversationId());
                assertEquals(rootEventId, event.getCorrelation().getParentEventId());
                assertEquals(requestId + ":" + childId + ":root", event.getCorrelation().getEventId());
                childRoots.add(event.getCorrelation().getEventId());
                childIds.add(childId);
            }
        }
        assertTrue(rootFound);
        assertEquals(new java.util.HashSet<String>(Arrays.asList("question-1", "question-2")), childIds);
        assertEquals(2, childRoots.size());
    }

    private static AskDataAuthorizationSummary allowedOrders(AskDataAuthorizationRequest request) {
        return AskDataAuthorizationSummary.allow(request, Collections.singletonList(
                new AskDataAuthorizedObject("orders", "orders", "*")), Collections.emptyList(),
                "policy-v1", "test-governance", AskDataAdministrativeScope.none());
    }

    private boolean hasChildAuditStep(AskDataResult child) {
        for (com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog step : child.getSteps()) {
            if ("child-audit".equals(step.getStepKey()) && step.isSuccess()) {
                return true;
            }
        }
        return false;
    }

    private void updateMax(AtomicInteger maximum, int current) {
        while (true) {
            int previous = maximum.get();
            if (previous >= current || maximum.compareAndSet(previous, current)) {
                return;
            }
        }
    }

    private interface QuestionExecutionProbe {
        void beforeModel(String question) throws InterruptedException;

        default void beforeSql(String question) {
        }
    }
}
