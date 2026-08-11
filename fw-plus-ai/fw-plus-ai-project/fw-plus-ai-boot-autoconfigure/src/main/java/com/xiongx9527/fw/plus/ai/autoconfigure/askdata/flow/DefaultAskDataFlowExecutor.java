package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCompressedContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelException;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlAuthorizationProcessor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataModelAttemptTrace;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetrievalException;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetrievalResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataMultiQuestionExecutor;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataMultiQuestionRequest;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataMultiQuestionResult;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepContext;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepKey;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepRegistry;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentStepResult;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraph;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphExecutor;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNode;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNodeExecutor;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlan;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataQuestionPlanner;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataSubQuestion;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataSubQuestionResult;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutionStatus;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlInterceptorChain;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResult;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationPurpose;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAuditStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataIdempotencyKeyStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultAskDataFlowExecutor implements AskDataFlowExecutor {

    private static final List<String> CHECKPOINT_STEPS = Arrays.asList("metadata", "prompt", "generate-sql",
            "validate-sql", "execute-sql");
    private static final List<String> AGENT_CHECKPOINT_STEPS = Arrays.asList("metadata", "merge-sub-agents", "prompt",
            "generate-sql", "validate-sql", "execute-sql");
    private static final String EXECUTION_EXECUTING = "EXECUTING";
    private static final String EXECUTION_COMPLETED = "COMPLETED";
    private static final String EXECUTION_SKIPPED = "SKIPPED";
    private static final String MULTI_QUESTION_OPTION = "questions";
    private static final String EMBEDDING_MODEL_OPTION = "embeddingModelKey";
    private static final String AUTHORIZATION_SUMMARY_ATTRIBUTE = "askDataAuthorizationSummary";
    private static final String OBSERVABILITY_REQUEST_ID_OPTION = "askDataObservabilityRequestId";
    private static final String OBSERVABILITY_SUB_QUESTION_ID_OPTION = "askDataObservabilitySubQuestionId";
    private static final String GOVERNANCE_ERROR_VERSION = "governance-error-v1";
    private static final String GOVERNANCE_ERROR_SOURCE = "governance-provider-error";

    private final AskDataProperties properties;
    private final AskDataModelRegistry modelRegistry;
    private final AskDataMetadataProvider metadataProvider;
    private final AskDataVectorRetriever vectorRetriever;
    private final AskDataPromptTemplateProvider promptTemplateProvider;
    private final AskDataDialectRegistry dialectRegistry;
    private final AskDataSqlValidator sqlValidator;
    private final AskDataSqlExecutor sqlExecutor;
    private final AskDataConversationStore conversationStore;
    private final AskDataContextCompressor contextCompressor;
    private final AskDataAgentDispatcher agentDispatcher;
    private final List<AskDataAgent> mainAgents;
    private final AskDataEventPublisher eventPublisher;
    private final AskDataQuestionPlanner questionPlanner;
    private final AskDataSqlInterceptorChain sqlInterceptorChain;
    private final com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator;
    private final AskDataGovernanceProvider governanceProvider;
    private final com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel;
    private final AskDataModelRouter coreModelRouter;
    private final AskDataAgentStepRegistry agentStepRegistry;
    private final AskDataRuntimeStateServices runtimeStateServices;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, null, null, null, Collections.<AskDataAgent>emptyList(), null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, null,
                Collections.<AskDataAgent>emptyList(), null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher,
                Collections.<AskDataAgent>emptyList(), null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, AskDataQuestionPlanner.simple());
    }

    /**
     * 创建支持问题计划的默认执行器。
     *
     * <p>规划器仅将问题划分为独立执行单元；每个单元仍复用同一项目和操作人上下文，不能改变隔离边界。</p>
     */
    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, sqlInterceptorChain, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, sqlInterceptorChain, coreSqlValidator, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator,
            AskDataGovernanceProvider governanceProvider) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, sqlInterceptorChain, coreSqlValidator, governanceProvider, null, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator,
            AskDataGovernanceProvider governanceProvider,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel,
            AskDataModelRouter coreModelRouter) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, sqlInterceptorChain, coreSqlValidator, governanceProvider,
                coreChatModel, coreModelRouter, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator,
            AskDataGovernanceProvider governanceProvider,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel,
            AskDataModelRouter coreModelRouter, AskDataAgentStepRegistry agentStepRegistry) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher, mainAgents,
                eventPublisher, questionPlanner, sqlInterceptorChain, coreSqlValidator, governanceProvider,
                coreChatModel, coreModelRouter, agentStepRegistry, null);
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents, AskDataEventPublisher eventPublisher,
            AskDataQuestionPlanner questionPlanner, AskDataSqlInterceptorChain sqlInterceptorChain,
            com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator coreSqlValidator,
            AskDataGovernanceProvider governanceProvider,
            com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel coreChatModel,
            AskDataModelRouter coreModelRouter, AskDataAgentStepRegistry agentStepRegistry,
            AskDataRuntimeStateServices runtimeStateServices) {
        this.properties = properties;
        this.modelRegistry = modelRegistry;
        this.metadataProvider = metadataProvider;
        this.vectorRetriever = vectorRetriever;
        this.promptTemplateProvider = promptTemplateProvider;
        this.dialectRegistry = dialectRegistry;
        this.sqlValidator = sqlValidator;
        this.sqlExecutor = sqlExecutor;
        this.conversationStore = conversationStore;
        this.contextCompressor = contextCompressor;
        this.agentDispatcher = agentDispatcher;
        this.mainAgents = mainAgents == null ? Collections.<AskDataAgent>emptyList()
                : new ArrayList<AskDataAgent>(mainAgents);
        this.eventPublisher = eventPublisher;
        this.questionPlanner = questionPlanner == null ? AskDataQuestionPlanner.simple() : questionPlanner;
        this.sqlInterceptorChain = sqlInterceptorChain;
        this.coreSqlValidator = coreSqlValidator;
        this.governanceProvider = governanceProvider == null ? new DefaultDenyAskDataGovernanceProvider()
                : governanceProvider;
        this.coreChatModel = coreChatModel;
        this.coreModelRouter = coreModelRouter;
        this.agentStepRegistry = agentStepRegistry;
        this.runtimeStateServices = runtimeStateServices;
    }

    @Override
    public AskDataResult execute(AskDataRequest request) {
        AskDataQuestionPlan plan;
        try {
            plan = plannedQuestions(request);
        } catch (IllegalArgumentException ex) {
            AskDataResult failed = failure(new AskDataResult(), "multi-question",
                    AskDataErrorCode.SUBQUESTION_FAILED, message(ex));
            publishLifecycleEvent(request, failed, AskDataEvent.Type.QUESTION_PLAN_FAILED,
                    "AskData 问题规划失败");
            return failed;
        }
        if (plan != null && plan.isMultiQuestion()) {
            return executeMultiple(request, plan);
        }
        return executeSingle(request);
    }

    /**
     * 执行兼容的单问题流程。
     *
     * <p>该路径保留原有的会话、恢复和步骤顺序；仅多问题计划会进入聚合流程。</p>
     */
    private AskDataResult executeSingle(AskDataRequest request) {
        return executeSingle(request, isResumeRequested(request));
    }

    private AskDataResult executeSingle(AskDataRequest request, boolean resumeRequested) {
        AskDataResult result = new AskDataResult();
        if (request != null) {
            result.setConversationId(request.getConversationId());
        }
        if (!hasValidProjectContext(request)) {
            return failure(result, "request", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData 请求缺少有效项目与操作人上下文");
        }
        AskDataAuthorizationSummary authorizationSummary = authorize(request, result);
        if (governanceProvider != null && authorizationSummary == null) {
            return result;
        }
        if (!resumeRequested && isBlank(result.getConversationId())) {
            result.setConversationId(UUID.randomUUID().toString());
            request.setConversationId(result.getConversationId());
        }
        String ownerId = instanceId();
        if (!recordIdempotency(request, result)) {
            return failure(result, "idempotency", AskDataErrorCode.SUBQUESTION_FAILED,
                    "AskData 幂等键已被同一项目和操作人使用");
        }
        if (!acquireLease(request, result, ownerId)) {
            return failure(result, "lease", AskDataErrorCode.RESUME_FAILED,
                    "AskData 会话正在由其他实例处理");
        }
        try {
            return executeSingleWithState(request, resumeRequested, result, authorizationSummary);
        } finally {
            releaseLease(request, result, ownerId);
        }
    }

    private AskDataResult executeSingleWithState(AskDataRequest request, boolean resumeRequested,
            AskDataResult result, AskDataAuthorizationSummary authorizationSummary) {
        String subQuestionId = observationOption(request, OBSERVABILITY_SUB_QUESTION_ID_OPTION);
        if (!isBlank(subQuestionId)) {
            result.setQuestionId(subQuestionId);
        }
        AskDataStepContext context = new AskDataStepContext(request);
        context.getAttributes().put(AUTHORIZATION_SUMMARY_ATTRIBUTE, authorizationSummary);
        publishLifecycleEvent(request, result, AskDataEvent.Type.CONVERSATION_STARTED, "AskData 对话已开始");
        publishLifecycleEvent(request, result, AskDataEvent.Type.QUESTION_PLANNED, "AskData 问题计划已完成");
        boolean rootSingleQuestion = isBlank(subQuestionId);
        if (rootSingleQuestion) {
            publishSubQuestionEvent(request, result, "question-1", AskDataEvent.Type.SUB_QUESTION_STARTED,
                    "AskData 子问题已开始");
        }
        AskDataResult completed;
        if (resumeRequested) {
            publishLifecycleEvent(request, result, AskDataEvent.Type.RECOVERY_ATTEMPTED, "AskData 恢复已尝试");
            completed = resume(request, result, context);
            publishLifecycleEvent(request, completed, completed.isSuccess() ? AskDataEvent.Type.RECOVERY_COMPLETED
                    : AskDataEvent.Type.RECOVERY_FAILED,
                    completed.isSuccess() ? "AskData 恢复已完成" : "AskData 恢复失败");
        } else {
            completed = executeNew(request, result, context);
        }
        if (rootSingleQuestion) {
            publishSubQuestionEvent(request, completed, "question-1",
                    completed.isSuccess() ? AskDataEvent.Type.SUB_QUESTION_COMPLETED
                            : AskDataEvent.Type.SUB_QUESTION_FAILED,
                    "AskData 子问题已进入终态");
        }
        publishLifecycleEvent(request, completed, completed.isSuccess() ? AskDataEvent.Type.CONVERSATION_COMPLETED
                : AskDataEvent.Type.CONVERSATION_FAILED, completed.isSuccess() ? "AskData 对话已完成" : "AskData 对话失败");
        saveAgentState(request, completed, completed.isSuccess() ? "COMPLETED" : "FAILED", "terminal");
        saveAuditState(request, completed);
        return completed;
    }

    /**
     * 将多问题计划拆分为相互独立的子请求。
     *
     * <p>子请求复用同一项目和操作人上下文，不会把其中任一问题的失败扩散到其他问题。每个子结果写入
     * 独立审计步骤，聚合结果仅汇总是否全部成功，不合并敏感的模型输出或 SQL。</p>
     */
    private AskDataResult executeMultiple(final AskDataRequest request, final AskDataQuestionPlan plan) {
        AskDataResult aggregated = new AskDataResult();
        if (!hasValidProjectContext(request)) {
            return failure(aggregated, "request", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData 请求缺少有效项目与操作人上下文");
        }
        final AskDataAuthorizationSummary authorizationSummary = authorize(request, aggregated);
        if (authorizationSummary == null) {
            return aggregated;
        }
        final boolean resumeRequested = isResumeRequested(request);
        AskDataCheckpoint parentCheckpoint = null;
        if (resumeRequested) {
            parentCheckpoint = loadMultiQuestionCheckpoint(request, aggregated, plan, authorizationSummary);
            if (parentCheckpoint == null) {
                return aggregated;
            }
            aggregated.setConversationId(parentCheckpoint.getConversationId());
            aggregated.setCheckpointId(parentCheckpoint.getCheckpointId());
            aggregated.setResumeToken(parentCheckpoint.getResumeToken());
        } else {
            aggregated.setConversationId(UUID.randomUUID().toString());
        }
        request.setConversationId(aggregated.getConversationId());
        String ownerId = instanceId();
        if (!recordIdempotency(request, aggregated)) {
            return failure(aggregated, "idempotency", AskDataErrorCode.SUBQUESTION_FAILED,
                    "AskData 多问题幂等键已被同一项目和操作人用于其他会话");
        }
        if (!acquireLease(request, aggregated, ownerId)) {
            return failure(aggregated, "lease", AskDataErrorCode.RESUME_FAILED,
                    "AskData 多问题会话正在由其他实例处理");
        }
        try {
            AskDataResult completed;
            try {
                completed = executeMultipleWithState(request, plan, aggregated, parentCheckpoint,
                        authorizationSummary);
            } catch (RuntimeException ex) {
                completed = failure(aggregated, "multi-question", AskDataErrorCode.SUBQUESTION_FAILED,
                        message(ex));
            }
            return completeMultiple(request, completed);
        } finally {
            releaseLease(request, aggregated, ownerId);
        }
    }

    private AskDataResult executeMultipleWithState(final AskDataRequest request, final AskDataQuestionPlan plan,
            final AskDataResult aggregated, AskDataCheckpoint parentCheckpoint,
            final AskDataAuthorizationSummary authorizationSummary) {
        publishLifecycleEvent(request, aggregated, AskDataEvent.Type.CONVERSATION_STARTED, "AskData 多问题对话已开始");
        publishLifecycleEvent(request, aggregated, AskDataEvent.Type.QUESTION_PLANNED, "AskData 多问题计划已完成");
        final ConcurrentMap<String, AskDataResult> childResults = new ConcurrentHashMap<String, AskDataResult>();
        final Map<String, AskDataSubQuestion> questionsById;
        try {
            questionsById = questionsById(plan);
        } catch (IllegalArgumentException ex) {
            return failure(aggregated, "multi-question", AskDataErrorCode.SUBQUESTION_FAILED, message(ex));
        }
        final List<AskDataSubQuestionResult> restored;
        try {
            restored = restoreMultiQuestionResults(parentCheckpoint, questionsById, childResults);
        } catch (IllegalArgumentException ex) {
            return failure(aggregated, "multi-question", AskDataErrorCode.RESUME_FAILED, message(ex));
        }
        AskDataResult initialCheckpointFailure = saveMultiQuestionCheckpoint(request, aggregated, plan, childResults,
                authorizationSummary);
        if (initialCheckpointFailure != null) {
            return initialCheckpointFailure;
        }
        AskDataMultiQuestionRequest multiRequest = new AskDataMultiQuestionRequest(request.getProjectContext(),
                aggregated.getConversationId(), plan.getSubQuestions(), multiQuestionTimeoutMillis(),
                multiQuestionMaxConcurrency(), restored);
        final AtomicReference<AskDataResult> checkpointFailure = new AtomicReference<AskDataResult>();
        AskDataMultiQuestionResult terminalResults;
        try {
            terminalResults = new AskDataMultiQuestionExecutor().execute(multiRequest, question -> {
                publishSubQuestionEvent(request, aggregated, question.getId(), AskDataEvent.Type.SUB_QUESTION_STARTED,
                        "AskData 子问题已开始");
                AskDataRequest childRequest = copyForQuestion(request, question, aggregated.getConversationId());
                boolean resumeChild = prepareChildResume(childRequest);
                AskDataResult child = executeSingle(childRequest, resumeChild);
                decorateChildResult(child, question,
                        child.isSuccess() ? AskDataSubQuestionResult.State.SUCCEEDED
                                : AskDataSubQuestionResult.State.FAILED);
                childResults.putIfAbsent(question.getId(), child);
                return toCoreResult(child);
            }, terminal -> {
                AskDataSubQuestion question = questionsById.get(terminal.getQuestionId());
                AskDataResult child = childResults.get(terminal.getQuestionId());
                if (child == null || !terminal.getState().name().equals(child.getQuestionState())) {
                    child = terminalChildResult(question, terminal, aggregated.getConversationId());
                    childResults.put(terminal.getQuestionId(), child);
                }
                publishSubQuestionEvent(request, aggregated, terminal.getQuestionId(),
                        subQuestionEventType(terminal.getState()), "AskData 子问题已进入终态");
                AskDataResult failed = saveMultiQuestionCheckpoint(request, aggregated, plan, childResults,
                        authorizationSummary);
                if (failed != null) {
                    checkpointFailure.compareAndSet(null, failed);
                    multiRequest.cancel();
                }
            });
        } catch (IllegalArgumentException ex) {
            return failure(aggregated, "multi-question", AskDataErrorCode.SUBQUESTION_FAILED, message(ex));
        }
        aggregated.setChildResults(orderedChildResults(plan, childResults, terminalResults,
                aggregated.getConversationId()));
        boolean allSucceeded = allChildrenSucceeded(aggregated.getChildResults());
        aggregated.setSuccess(allSucceeded);
        if (!allSucceeded) {
            AskDataResult firstFailure = firstFailedChild(aggregated.getChildResults());
            aggregated.setErrorCode(firstFailure == null || isBlank(firstFailure.getErrorCode())
                    ? AskDataErrorCode.SUBQUESTION_FAILED : firstFailure.getErrorCode());
            aggregated.setErrorMessage(firstFailure == null ? "One or more child questions failed"
                    : firstFailure.getErrorMessage());
        }
        aggregated.addStep("multi-question", "Multi question", allSucceeded,
                allSucceeded ? "All child questions completed" : "One or more child questions failed");
        AskDataResult finalCheckpointFailure = saveMultiQuestionCheckpoint(request, aggregated, plan, childResults,
                authorizationSummary);
        if (checkpointFailure.get() != null) {
            AskDataResult failed = checkpointFailure.get();
            failed.setChildResults(aggregated.getChildResults());
            return failed;
        }
        AskDataResult completed = finalCheckpointFailure == null ? aggregated : finalCheckpointFailure;
        return completed;
    }

    private AskDataResult completeMultiple(AskDataRequest request, AskDataResult completed) {
        publishLifecycleEvent(request, completed, completed.isSuccess() ? AskDataEvent.Type.CONVERSATION_COMPLETED
                : AskDataEvent.Type.CONVERSATION_FAILED,
                completed.isSuccess() ? "AskData 多问题对话已完成" : "AskData 多问题对话失败");
        saveAgentState(request, completed, completed.isSuccess() ? "COMPLETED" : "FAILED", "terminal");
        saveAuditState(request, completed);
        return completed;
    }

    private AskDataRequest copyForQuestion(AskDataRequest source, AskDataSubQuestion question,
            String parentConversationId) {
        AskDataRequest target = new AskDataRequest();
        target.setQuestion(question.getQuestion());
        target.setProjectContext(source.getProjectContext());
        target.setDatasourceKey(source.getDatasourceKey());
        target.setDialect(source.getDialect());
        target.setModelKey(source.getModelKey());
        target.setConversationId(parentConversationId + ":" + question.getId());
        target.setExecute(source.getExecute());
        target.setMaxRows(source.getMaxRows());
        Map<String, Object> options = source.getOptions() == null ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(source.getOptions());
        Object parentIdempotencyKey = options.get("idempotencyKey");
        if (parentIdempotencyKey instanceof String && !isBlank((String) parentIdempotencyKey)) {
            options.put("idempotencyKey", ((String) parentIdempotencyKey).trim() + ":" + question.getId());
        }
        options.put(OBSERVABILITY_REQUEST_ID_OPTION, parentConversationId);
        options.put(OBSERVABILITY_SUB_QUESTION_ID_OPTION, question.getId());
        target.setOptions(options);
        return target;
    }

    private AskDataQuestionPlan plannedQuestions(AskDataRequest request) {
        if (request == null) {
            return null;
        }
        AskDataQuestionPlan explicit = explicitQuestionPlan(request);
        AskDataQuestionPlan plan = explicit == null ? questionPlanner.plan(request.getQuestion()) : explicit;
        if (plan == null) {
            throw new IllegalArgumentException("AskData 问题计划不能为空");
        }
        return plan;
    }

    private AskDataQuestionPlan explicitQuestionPlan(AskDataRequest request) {
        if (request.getOptions() == null || !request.getOptions().containsKey(MULTI_QUESTION_OPTION)) {
            return null;
        }
        Object configuredQuestions = request.getOptions().get(MULTI_QUESTION_OPTION);
        if (!(configuredQuestions instanceof Iterable<?>)) {
            throw new IllegalArgumentException("questions 必须是字符串或结构化子问题列表");
        }
        List<String> strings = new ArrayList<String>();
        List<AskDataSubQuestion> structured = new ArrayList<AskDataSubQuestion>();
        for (Object configuredQuestion : (Iterable<?>) configuredQuestions) {
            if (configuredQuestion instanceof String) {
                if (!structured.isEmpty() || isBlank((String) configuredQuestion)) {
                    throw new IllegalArgumentException("questions 不能混用字符串与结构化子问题");
                }
                strings.add(((String) configuredQuestion).trim());
            } else if (configuredQuestion instanceof Map<?, ?>) {
                if (!strings.isEmpty()) {
                    throw new IllegalArgumentException("questions 不能混用字符串与结构化子问题");
                }
                structured.add(structuredQuestion((Map<?, ?>) configuredQuestion));
            } else {
                throw new IllegalArgumentException("questions 包含不支持的子问题类型");
            }
        }
        if (!structured.isEmpty()) {
            return AskDataQuestionPlan.ofSubQuestions(structured);
        }
        if (!strings.isEmpty()) {
            return questionPlanner.plan(strings);
        }
        throw new IllegalArgumentException("questions 不能为空");
    }

    private AskDataSubQuestion structuredQuestion(Map<?, ?> value) {
        String id = requiredString(value.get("id"), "question.id");
        String question = requiredString(value.get("question"), "question.question");
        List<String> dependsOn = stringList(value.get("dependsOn"), "question.dependsOn");
        AskDataSubQuestion.FailurePolicy failurePolicy = failurePolicy(value.get("failurePolicy"));
        long timeoutMillis = timeoutMillis(value.get("timeoutMillis"));
        return new AskDataSubQuestion(id, question, dependsOn, failurePolicy, timeoutMillis);
    }

    private String requiredString(Object value, String name) {
        if (!(value instanceof String) || isBlank((String) value)) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return ((String) value).trim();
    }

    private List<String> stringList(Object value, String name) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof Iterable<?>)) {
            throw new IllegalArgumentException(name + " 必须是字符串列表");
        }
        List<String> result = new ArrayList<String>();
        for (Object entry : (Iterable<?>) value) {
            result.add(requiredString(entry, name));
        }
        return result;
    }

    private AskDataSubQuestion.FailurePolicy failurePolicy(Object value) {
        if (value == null) {
            return AskDataSubQuestion.FailurePolicy.SKIP;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("question.failurePolicy 必须是 SKIP 或 CONTINUE");
        }
        String policy = ((String) value).trim();
        if ("SKIP".equalsIgnoreCase(policy)) {
            return AskDataSubQuestion.FailurePolicy.SKIP;
        }
        if ("CONTINUE".equalsIgnoreCase(policy)) {
            return AskDataSubQuestion.FailurePolicy.CONTINUE;
        }
        throw new IllegalArgumentException("question.failurePolicy 必须是 SKIP 或 CONTINUE");
    }

    private long timeoutMillis(Object value) {
        if (value == null) {
            return 0L;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("question.timeoutMillis 必须是非负整数");
        }
        long timeout = ((Number) value).longValue();
        if (timeout < 0L) {
            throw new IllegalArgumentException("question.timeoutMillis 不能为负数");
        }
        return timeout;
    }

    private Map<String, AskDataSubQuestion> questionsById(AskDataQuestionPlan plan) {
        Map<String, AskDataSubQuestion> result = new LinkedHashMap<String, AskDataSubQuestion>();
        for (AskDataSubQuestion question : plan.getSubQuestions()) {
            if (result.put(question.getId(), question) != null) {
                throw new IllegalArgumentException("子问题 ID 不可重复: " + question.getId());
            }
        }
        return result;
    }

    private AskDataCheckpoint loadMultiQuestionCheckpoint(AskDataRequest request, AskDataResult result,
            AskDataQuestionPlan plan, AskDataAuthorizationSummary authorizationSummary) {
        if (conversationStore == null) {
            failure(result, "multi-question", AskDataErrorCode.RESUME_FAILED,
                    "AskData conversation store is not configured");
            return null;
        }
        try {
            AskDataProjectContext projectContext = request.getProjectContext();
            AskDataCheckpoint checkpoint = !isBlank(request.getResumeToken())
                    ? conversationStore.loadByResumeToken(projectContext, request.getResumeToken())
                    : conversationStore.loadLatest(projectContext, request.getConversationId());
            if (checkpoint == null) {
                failure(result, "multi-question", AskDataErrorCode.CHECKPOINT_NOT_FOUND,
                        "AskData multi-question checkpoint was not found");
                return null;
            }
            AskDataCheckpoint latest = conversationStore.loadLatest(projectContext, checkpoint.getConversationId());
            if (latest != null) {
                checkpoint = latest;
            }
            if (!Boolean.TRUE.equals(checkpoint.getMultiQuestion())
                    || !equals(checkpointVersion(), checkpoint.getVersion())
                    || !equals(multiQuestionPlanSnapshot(plan), checkpoint.getMultiQuestionPlanSnapshot())
                    || !requestIdentityMatches(request, checkpoint, authorizationSummary)) {
                failure(result, "multi-question", AskDataErrorCode.RESUME_FAILED,
                        "AskData multi-question checkpoint identity or version does not match");
                return null;
            }
            return checkpoint;
        } catch (RuntimeException ex) {
            failure(result, "multi-question", AskDataErrorCode.RESUME_FAILED, message(ex));
            return null;
        }
    }

    private List<AskDataSubQuestionResult> restoreMultiQuestionResults(AskDataCheckpoint checkpoint,
            Map<String, AskDataSubQuestion> questionsById, ConcurrentMap<String, AskDataResult> childResults) {
        if (checkpoint == null) {
            return Collections.emptyList();
        }
        List<AskDataSubQuestionResult> restored = new ArrayList<AskDataSubQuestionResult>();
        for (AskDataResult child : checkpoint.getChildResults()) {
            if (child == null || isBlank(child.getQuestionId()) || isBlank(child.getQuestionState())) {
                throw new IllegalArgumentException("父检查点包含无效子问题终态");
            }
            AskDataSubQuestion question = questionsById.get(child.getQuestionId());
            if (question == null || childResults.putIfAbsent(child.getQuestionId(), child) != null) {
                throw new IllegalArgumentException("父检查点子问题 ID 与当前计划不匹配");
            }
            child.setDependsOn(question.getDependsOn());
            restored.add(toCoreResult(child));
        }
        for (AskDataSubQuestionResult terminal : restored) {
            AskDataSubQuestion question = questionsById.get(terminal.getQuestionId());
            if (!childResults.keySet().containsAll(question.getDependsOn())) {
                throw new IllegalArgumentException("父检查点子问题缺少前置依赖终态: " + terminal.getQuestionId());
            }
        }
        return restored;
    }

    private boolean prepareChildResume(AskDataRequest childRequest) {
        if (conversationStore == null) {
            return false;
        }
        AskDataCheckpoint childCheckpoint = conversationStore.loadLatest(childRequest.getProjectContext(),
                childRequest.getConversationId());
        if (childCheckpoint == null) {
            return false;
        }
        childRequest.setResumeToken(childCheckpoint.getResumeToken());
        return true;
    }

    private void decorateChildResult(AskDataResult child, AskDataSubQuestion question,
            AskDataSubQuestionResult.State state) {
        child.setQuestionId(question.getId());
        child.setQuestionState(state.name());
        child.setDependsOn(question.getDependsOn());
        child.addStep("child-audit", "Child audit", child.isSuccess(), "Child question completed");
    }

    private AskDataSubQuestionResult toCoreResult(AskDataResult child) {
        String questionId = child.getQuestionId();
        String state = child.getQuestionState();
        if (AskDataSubQuestionResult.State.SUCCEEDED.name().equals(state)) {
            return AskDataSubQuestionResult.success(questionId, child.getBrief());
        }
        if (AskDataSubQuestionResult.State.FAILED.name().equals(state)) {
            return AskDataSubQuestionResult.failed(questionId,
                    isBlank(child.getErrorCode()) ? AskDataErrorCode.SUBQUESTION_FAILED : child.getErrorCode(),
                    child.getErrorMessage());
        }
        if (AskDataSubQuestionResult.State.TIMED_OUT.name().equals(state)) {
            return AskDataSubQuestionResult.timedOut(questionId);
        }
        if (AskDataSubQuestionResult.State.CANCELLED.name().equals(state)) {
            return AskDataSubQuestionResult.cancelled(questionId);
        }
        if (AskDataSubQuestionResult.State.SKIPPED.name().equals(state)) {
            return AskDataSubQuestionResult.skipped(questionId);
        }
        throw new IllegalArgumentException("未知子问题终态: " + state);
    }

    private AskDataResult terminalChildResult(AskDataSubQuestion question, AskDataSubQuestionResult terminal,
            String parentConversationId) {
        AskDataResult child = AskDataResult.failure(terminal.getErrorCode(), terminal.getErrorMessage());
        child.setConversationId(parentConversationId + ":" + terminal.getQuestionId());
        decorateChildResult(child, question, terminal.getState());
        return child;
    }

    private List<AskDataResult> orderedChildResults(AskDataQuestionPlan plan,
            ConcurrentMap<String, AskDataResult> childResults, AskDataMultiQuestionResult terminalResults,
            String parentConversationId) {
        List<AskDataResult> ordered = new ArrayList<AskDataResult>();
        for (AskDataSubQuestion question : plan.getSubQuestions()) {
            AskDataResult child = childResults.get(question.getId());
            if (child == null) {
                child = terminalChildResult(question, terminalResults.getResult(question.getId()),
                        parentConversationId);
            }
            ordered.add(child);
        }
        return ordered;
    }

    private boolean allChildrenSucceeded(List<AskDataResult> children) {
        for (AskDataResult child : children) {
            if (!child.isSuccess() || !AskDataSubQuestionResult.State.SUCCEEDED.name().equals(child.getQuestionState())) {
                return false;
            }
        }
        return true;
    }

    private AskDataResult firstFailedChild(List<AskDataResult> children) {
        for (AskDataResult child : children) {
            if (!child.isSuccess()) {
                return child;
            }
        }
        return null;
    }

    private AskDataResult saveMultiQuestionCheckpoint(AskDataRequest request, AskDataResult result,
            AskDataQuestionPlan plan, Map<String, AskDataResult> childResults,
            AskDataAuthorizationSummary authorizationSummary) {
        if (conversationStore == null || isBlank(result.getConversationId())) {
            return null;
        }
        try {
            AskDataCheckpoint checkpoint = new AskDataCheckpoint();
            checkpoint.setConversationId(result.getConversationId());
            checkpoint.setProjectId(request.getProjectContext().getProjectId());
            checkpoint.setOperatorId(request.getProjectContext().getOperatorId());
            checkpoint.setAuthorizationVersion(authorizationSummary.getAuthorizationVersion());
            checkpoint.setCheckpointId(UUID.randomUUID().toString());
            checkpoint.setResumeToken(UUID.randomUUID().toString());
            checkpoint.setRequestSummary(request.getQuestion());
            checkpoint.setModelKey(request.getModelKey());
            checkpoint.setDatasourceKey(request.getDatasourceKey());
            checkpoint.setDialect(request.getDialect());
            checkpoint.setMaxRows(Integer.valueOf(maxRows(request)));
            checkpoint.setExecute(Boolean.valueOf(isExecutionEnabled(request)));
            checkpoint.setRequestOptionsSnapshot(checkpointScopeSnapshot(request));
            checkpoint.setMultiQuestion(Boolean.TRUE);
            checkpoint.setMultiQuestionPlanSnapshot(multiQuestionPlanSnapshot(plan));
            checkpoint.setChildResults(availableChildResults(plan, childResults));
            if (checkpoint.getChildResults().size() == plan.getSubQuestions().size()) {
                checkpoint.setCompletedSteps(Collections.singletonList("multi-question"));
            }
            checkpoint.setCurrentStep("multi-question");
            checkpoint.setSteps(result.getSteps());
            checkpoint.setErrorCode(result.getErrorCode());
            checkpoint.setVersion(checkpointVersion());
            checkpoint.setCreatedAt(new Date());
            publishStepEvent(request, result, AskDataEvent.Type.CHECKPOINT_SAVE_STARTED, "multi-question", null,
                    "AskData 多问题检查点保存已开始");
            AskDataCheckpoint saved = conversationStore.save(request.getProjectContext(), checkpoint);
            AskDataCheckpoint responseCheckpoint = saved == null ? checkpoint : saved;
            result.setCheckpointId(responseCheckpoint.getCheckpointId());
            result.setResumeToken(responseCheckpoint.getResumeToken());
            publishStepEvent(request, result, AskDataEvent.Type.CHECKPOINT_SAVED, "multi-question", null,
                    "AskData 多问题检查点已保存");
            return null;
        } catch (RuntimeException ex) {
            publishStepEvent(request, result, AskDataEvent.Type.CHECKPOINT_SAVE_FAILED, "multi-question", null,
                    "AskData 多问题检查点保存失败");
            AskDataResult failed = AskDataResult.failure(AskDataErrorCode.CONTEXT_COMPRESSION_FAILED, message(ex));
            failed.setConversationId(result.getConversationId());
            failed.setCheckpointId(result.getCheckpointId());
            failed.setResumeToken(result.getResumeToken());
            failed.setChildResults(availableChildResults(plan, childResults));
            failed.addStep("multi-question", "multi-question", false, message(ex));
            return failed;
        }
    }

    private List<AskDataResult> availableChildResults(AskDataQuestionPlan plan,
            Map<String, AskDataResult> childResults) {
        List<AskDataResult> available = new ArrayList<AskDataResult>();
        for (AskDataSubQuestion question : plan.getSubQuestions()) {
            AskDataResult child = childResults.get(question.getId());
            if (child != null) {
                available.add(child);
            }
        }
        return available;
    }

    private String multiQuestionPlanSnapshot(AskDataQuestionPlan plan) {
        StringBuilder snapshot = new StringBuilder("[");
        for (AskDataSubQuestion question : plan.getSubQuestions()) {
            if (snapshot.length() > 1) {
                snapshot.append(',');
            }
            snapshot.append("{\"id\":").append(jsonString(question.getId()))
                    .append(",\"question\":").append(jsonString(question.getQuestion()))
                    .append(",\"dependsOn\":[");
            for (int index = 0; index < question.getDependsOn().size(); index++) {
                if (index > 0) {
                    snapshot.append(',');
                }
                snapshot.append(jsonString(question.getDependsOn().get(index)));
            }
            snapshot.append("],\"failurePolicy\":").append(jsonString(question.getFailurePolicy().name()))
                    .append(",\"timeoutMillis\":").append(question.getTimeoutMillis()).append('}');
        }
        return snapshot.append(']').toString();
    }

    private int multiQuestionMaxConcurrency() {
        Integer value = properties.getFlow().getMultiQuestionMaxConcurrency();
        return value == null || value.intValue() <= 0 ? 1 : value.intValue();
    }

    private long multiQuestionTimeoutMillis() {
        Integer value = properties.getFlow().getMultiQuestionTimeoutMillis();
        return value == null || value.intValue() <= 0 ? 30000L : value.longValue();
    }

    private AskDataResult executeNew(AskDataRequest request, AskDataResult result, AskDataStepContext context) {
        AskDataMetadata metadata;
        try {
            metadata = metadataProvider.load(request, authorizationSummary(context));
        } catch (RuntimeException ex) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
        }
        if (metadata == null) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, "AskData metadata was not found");
        }
        metadata = authorizedMetadata(request, metadata, authorizationSummary(context));
        if (!hasAuthorizedMetadata(metadata)) {
            return governanceDeniedForMetadata(request, result, authorizationSummary(context));
        }
        context.setMetadata(metadata);
        result.addStep("metadata", "Metadata", true, "Metadata loaded");
        publishStepEvent(request, result, AskDataEvent.Type.METADATA_RESOLVED, "metadata", null,
                "AskData 元数据已解析");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "metadata", null, null);
        if (checkpointFailure != null) {
            return checkpointFailure;
        }
        metadata = selectTables(request, result, context, metadata);
        if (metadata == null) {
            return result;
        }
        context.setMetadata(metadata);

        AskDataResult agentFailure = runAgentsAndSaveCheckpoint(context, result);
        if (agentFailure != null) {
            return agentFailure;
        }

        return generateSqlAndValidate(request, result, context, metadata);
    }

    private AskDataMetadata selectTables(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataMetadata metadata) {
        return selectTables(request, result, context, metadata, true);
    }

    private AskDataMetadata selectTables(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataMetadata metadata, boolean recordStep) {
        com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent agent =
                stepAgent(AskDataAgentStepKey.SELECT_TABLES);
        if (agent == null) {
            return metadata;
        }
        AskDataAgentStepResult agentResult;
        try {
            agentResult = executeStepAgent(agent, agentContext(AskDataAgentStepKey.SELECT_TABLES,
                    request, result, metadata, null, null, null));
        } catch (RuntimeException ex) {
            failure(result, AskDataAgentStepKey.SELECT_TABLES, AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
            return null;
        }
        if (!agentResult.isSuccess()) {
            agentFailure(result, AskDataAgentStepKey.SELECT_TABLES, agentResult);
            return null;
        }
        Set<String> selectedResources = new LinkedHashSet<String>(agentResult.getSelectedResources());
        Set<String> authorizedResources = new LinkedHashSet<String>(metadataResources(metadata));
        if (selectedResources.isEmpty() || !authorizedResources.containsAll(selectedResources)) {
            failure(result, AskDataAgentStepKey.SELECT_TABLES, DefaultDenyAskDataGovernanceProvider.DENIAL_CODE,
                    "select-tables Agent 只能收窄治理后授权表");
            return null;
        }
        List<AskDataTable> selectedTables = new ArrayList<AskDataTable>();
        for (AskDataTable table : metadata.getTables()) {
            if (table != null && selectedResources.contains(table.getName())) {
                selectedTables.add(table);
            }
        }
        AskDataMetadata selectedMetadata = new AskDataMetadata(metadata.getDialect(), selectedTables,
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
        result.setTables(new ArrayList<String>(selectedResources));
        if (recordStep) {
            result.addStep(AskDataAgentStepKey.SELECT_TABLES, "Select tables", true, "Authorized tables selected");
        }
        return selectedMetadata;
    }

    private AskDataResult runAgentsAndSaveCheckpoint(AskDataStepContext context, AskDataResult result) {
        if (agentDispatcher == null && mainAgents.isEmpty()) {
            return null;
        }
        final Map<String, GraphNodeAction> actions = new LinkedHashMap<String, GraphNodeAction>();
        List<AskDataAgentNode> nodes = new ArrayList<AskDataAgentNode>();
        String previousNodeId = null;
        for (int index = 0; index < mainAgents.size(); index++) {
            final AskDataAgent agent = mainAgents.get(index);
            final String nodeId = "main-agent:" + index + ":" + agentName(agent);
            nodes.add(graphNode(nodeId, previousNodeId));
            actions.put(nodeId, new GraphNodeAction() {
                @Override
                public String execute() {
                    return executeMainAgent(agent, context, result);
                }
            });
            previousNodeId = nodeId;
        }
        final AtomicReference<AskDataDispatchResult> dispatchResult = new AtomicReference<AskDataDispatchResult>();
        if (agentDispatcher != null) {
            final String nodeId = "sub-agent-dispatcher";
            nodes.add(graphNode(nodeId, previousNodeId));
            actions.put(nodeId, new GraphNodeAction() {
                @Override
                public String execute() {
                    AskDataDispatchResult dispatched = dispatchSubAgents(context, result);
                    dispatchResult.set(dispatched);
                    return nodeId;
                }
            });
        }
        executeAgentGraph(context, result, nodes, actions);
        AskDataDispatchResult dispatched = dispatchResult.get();
        result.addStep("merge-sub-agents", "Merge agents", true,
                dispatched == null ? "Agent outputs merged" : "Merged " + dispatched.getResults().size()
                        + " sub-agent result(s)");
        return saveCheckpoint(context, result, "merge-sub-agents", null, null);
    }

    private void executeAgentGraph(AskDataStepContext context, AskDataResult result, List<AskDataAgentNode> nodes,
            final Map<String, GraphNodeAction> actions) {
        final Set<String> executedNodeIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        AskDataAgentNodeExecutor nodeExecutor = new AskDataAgentNodeExecutor() {
            @Override
            public String execute(AskDataAgentNode node) {
                GraphNodeAction action = node == null ? null : actions.get(node.getId());
                if (action == null) {
                    throw new IllegalArgumentException("图运行时提交了未知 Agent 节点: "
                            + (node == null ? "null" : node.getId()));
                }
                if (!executedNodeIds.add(node.getId())) {
                    throw new IllegalStateException("图运行时重复执行 Agent 节点: " + node.getId());
                }
                return action.execute();
            }
        };
        AskDataAgentGraph graph = new AskDataAgentGraph("askdata-agent-flow", "1", nodes);
        AskDataAgentGraphRuntime runtime = runtimeStateServices == null ? null
                : runtimeStateServices.getGraphRuntime();
        if (runtime == null) {
            runtime = new AskDataAgentGraphExecutor();
        }
        try {
            if (!runtime.isAvailable()) {
                throw new IllegalStateException("Agent 图运行时不可用: " + runtime.getKey());
            }
            runtime.execute(graph, nodeExecutor);
            if (executedNodeIds.size() != nodes.size()) {
                throw new IllegalStateException("Agent 图运行时未执行全部节点: runtime=" + runtime.getKey()
                        + "，expected=" + nodes.size() + "，actual=" + executedNodeIds.size());
            }
        } catch (RuntimeException ex) {
            if (executedNodeIds.isEmpty() && !(runtime instanceof AskDataAgentGraphExecutor)) {
                publishStepEvent(context.getRequest(), result, AskDataEvent.Type.GRAPH_RUNTIME_FALLBACK,
                        "agent-graph", runtime.getKey(), "AskData 图运行时不可用，已回退顺序执行");
                new AskDataAgentGraphExecutor().execute(graph, nodeExecutor);
                return;
            }
            result.addStep("agent-graph:" + runtime.getKey(), "Agent graph runtime", false, message(ex));
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.AGENT_NODE_FAILED,
                    "agent-graph", runtime.getKey(), "AskData 图运行时执行失败");
        }
    }

    private AskDataAgentNode graphNode(String nodeId, String previousNodeId) {
        return new AskDataAgentNode(nodeId, previousNodeId == null ? Collections.<String>emptyList()
                : Collections.singletonList(previousNodeId));
    }

    private String executeMainAgent(AskDataAgent agent, AskDataStepContext context, AskDataResult result) {
        String name = agentName(agent);
        try {
            AskDataStepResult stepResult = agent == null ? null : agent.execute(context);
            boolean success = stepResult != null && stepResult.isSuccess();
            String agentMessage = stepResult == null ? "Main agent returned no result"
                    : success ? stepResult.getOutputSummary() : stepResult.getErrorMessage();
            context.getAttributes().put("askData.mainAgent." + name, agentMessage);
            result.addStep("agent:" + name, "Agent " + name, success, agentMessage);
            publishStepEvent(context.getRequest(), result,
                    success ? AskDataEvent.Type.AGENT_NODE_EXECUTED : AskDataEvent.Type.AGENT_NODE_FAILED,
                    "agent:" + name, name, success ? "AskData Agent 已完成" : "AskData Agent 执行失败");
            return agentMessage;
        } catch (RuntimeException ex) {
            String failureMessage = message(ex);
            context.getAttributes().put("askData.mainAgent." + name, failureMessage);
            result.addStep("agent:" + name, "Agent " + name, false, failureMessage);
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.AGENT_NODE_FAILED,
                    "agent:" + name, name, "AskData Agent 执行失败");
            return failureMessage;
        }
    }

    private AskDataDispatchResult dispatchSubAgents(AskDataStepContext context, AskDataResult result) {
        AskDataProperties.Flow flow = properties == null ? null : properties.getFlow();
        Integer timeout = flow == null ? null : flow.getDefaultSubAgentTimeoutMillis();
        AskDataDispatchResult dispatchResult;
        try {
            dispatchResult = agentDispatcher.dispatch(new AskDataDispatchRequest(
                    Collections.<AskDataSubAgent>emptyList(), context,
                    timeout == null ? 0L : timeout.longValue(), AskDataDispatchRequest.FailurePolicy.CONTINUE, null));
        } catch (RuntimeException ex) {
            result.addStep("sub-agent:dispatcher", "Sub-agent dispatcher", false, message(ex));
            return null;
        }
        if (dispatchResult == null) {
            return null;
        }
        context.getAttributes().put("askData.subAgentResults", dispatchResult.getResults());
        for (AskDataSubAgentResult subAgentResult : dispatchResult.getResults()) {
            String name = subAgentResult.getAgentName() == null ? "unknown" : subAgentResult.getAgentName();
            String agentMessage = subAgentResult.isSuccess() ? subAgentResult.getOutputSummary()
                    : subAgentResult.getErrorMessage();
            if (subAgentResult.isTimedOut()) {
                agentMessage = "Timed out: " + agentMessage;
            }
            context.getAttributes().put("askData.subAgent." + name, agentMessage);
            result.addStep("sub-agent:" + name, "Sub-agent " + name, subAgentResult.isSuccess(), agentMessage);
            publishStepEvent(context.getRequest(), result,
                    subAgentResult.isSuccess() ? AskDataEvent.Type.AGENT_NODE_EXECUTED
                            : AskDataEvent.Type.AGENT_NODE_FAILED,
                    "sub-agent:" + name, name,
                    subAgentResult.isSuccess() ? "AskData 子 Agent 已完成" : "AskData 子 Agent 执行失败");
        }
        return dispatchResult;
    }

    private String agentName(AskDataAgent agent) {
        return agent == null || isBlank(agent.getName()) ? "unknown" : agent.getName().trim();
    }

    private interface GraphNodeAction {
        String execute();
    }

    private com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent stepAgent(String stepKey) {
        return agentStepRegistry == null ? null : agentStepRegistry.get(stepKey);
    }

    private AskDataAgentStepResult executeStepAgent(
            com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent agent,
            AskDataAgentStepContext agentContext) {
        try {
            AskDataAgentStepResult agentResult = agent.execute(agentContext);
            if (agentResult == null) {
                throw new IllegalArgumentException("Agent " + agent.getKey() + " 未返回步骤结果");
            }
            publishAgentStepEvent(agent, agentContext, agentResult.isSuccess()
                    ? AskDataEvent.Type.AGENT_NODE_EXECUTED : AskDataEvent.Type.AGENT_NODE_FAILED);
            return agentResult;
        } catch (RuntimeException ex) {
            publishAgentStepEvent(agent, agentContext, AskDataEvent.Type.AGENT_NODE_FAILED);
            throw ex;
        }
    }

    private AskDataAgentStepContext agentContext(String stepKey, AskDataRequest request, AskDataResult result,
            AskDataMetadata metadata, String prompt, AskDataSqlCommand sqlCommand, AskDataSqlResult sqlResult) {
        com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata coreMetadata = metadata == null ? null
                : new com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata(request.getDatasourceKey(),
                        metadataResources(metadata));
        return AskDataAgentStepContext.builder(stepKey, request.getProjectContext(), request.getQuestion())
                .datasourceKey(request.getDatasourceKey())
                .conversationId(result.getConversationId())
                .metadata(coreMetadata)
                .selectedResources(metadataResources(metadata))
                .prompt(prompt)
                .sqlCommand(sqlCommand)
                .sqlResult(sqlResult)
                .rowLimit(maxRows(request))
                .timeoutMillis(queryTimeoutMillis())
                .build();
    }

    private List<String> metadataResources(AskDataMetadata metadata) {
        if (metadata == null || metadata.getTables().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> resources = new ArrayList<String>();
        for (AskDataTable table : metadata.getTables()) {
            if (table != null && !isBlank(table.getName())) {
                resources.add(table.getName());
            }
        }
        return resources;
    }

    private AskDataResult agentFailure(AskDataResult result, String stepKey, AskDataAgentStepResult agentResult) {
        String errorCode = isBlank(agentResult.getErrorCode())
                ? AskDataErrorCode.SUBQUESTION_FAILED : agentResult.getErrorCode();
        String errorMessage = isBlank(agentResult.getErrorMessage())
                ? stepKey + " Agent 执行失败" : agentResult.getErrorMessage();
        return failure(result, stepKey, errorCode, errorMessage);
    }

    private AskDataResult generateSqlAndValidate(AskDataRequest request, AskDataResult result,
            AskDataStepContext context, AskDataMetadata metadata) {
        int maxRows = maxRows(request);
        List<AskDataRetrievedDocument> documents;
        try {
            documents = retrieveDocuments(request, authorizationSummary(context), result);
        } catch (RuntimeException ex) {
            return failure(result, "prompt", AskDataErrorCode.VECTOR_RETRIEVAL_FAILED, message(ex));
        }
        documents.addAll(recallLongTermMemory(request, result, authorizationSummary(context)));
        context.setRetrievedDocuments(documents);
        String prompt;
        try {
            prompt = promptTemplateProvider.buildPrompt(request, metadata, documents, maxRows);
        } catch (RuntimeException ex) {
            return failure(result, "prompt", AskDataErrorCode.MODEL_RESPONSE_INVALID, message(ex));
        }
        context.setPrompt(prompt);
        result.addStep("prompt", "Prompt", true, "Prompt built");
        publishStepEvent(request, result, AskDataEvent.Type.PROMPT_BUILT, "prompt", null,
                "AskData Prompt 已构建");

        String generatedSql;
        com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent generateAgent =
                stepAgent(AskDataAgentStepKey.GENERATE_SQL);
        if (generateAgent == null) {
            AskDataModelResponse modelResponse;
            try {
                modelResponse = generateModelResponse(request, prompt, result);
            } catch (AskDataModelException ex) {
                return failure(result, "generate-sql", ex.getErrorCode(), message(ex));
            } catch (RuntimeException ex) {
                return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID, message(ex));
            }
            if (modelResponse == null || isBlank(modelResponse.getText())) {
                return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                        "AskData model response is empty");
            }
            context.setRawModelResponse(modelResponse.getText());
            result.setRawModelResponse(modelResponse.getText());
            JsonNode modelJson;
            try {
                modelJson = objectMapper.readTree(modelResponse.getText());
            } catch (Exception ex) {
                return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                        "AskData model response is not valid JSON");
            }
            generatedSql = textValue(modelJson, "sql");
            if (isBlank(generatedSql)) {
                return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                        "AskData model response does not contain SQL");
            }
            result.setTables(textValues(modelJson.get("tables")));
            result.setChartType(textValue(modelJson, "chartType"));
            result.setBrief(textValue(modelJson, "brief"));
        } else {
            AskDataAgentStepResult agentResult;
            try {
                agentResult = executeStepAgent(generateAgent, agentContext(AskDataAgentStepKey.GENERATE_SQL,
                        request, result, metadata, prompt, null, null));
            } catch (RuntimeException ex) {
                return failure(result, "generate-sql", AskDataErrorCode.SUBQUESTION_FAILED, message(ex));
            }
            if (!agentResult.isSuccess()) {
                return agentFailure(result, AskDataAgentStepKey.GENERATE_SQL, agentResult);
            }
            generatedSql = agentResult.getSql();
            if (isBlank(generatedSql)) {
                return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                        "generate-sql Agent 未返回 SQL");
            }
        }
        context.setSql(generatedSql);
        result.addStep("generate-sql", "Generate SQL", true, "SQL generated");
        publishStepEvent(request, result, AskDataEvent.Type.SQL_COMPOSED, "generate-sql",
                generateAgent == null ? null : generateAgent.getKey(), "AskData SQL 已组装");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "generate-sql", null, null);
        if (checkpointFailure != null) {
            return checkpointFailure;
        }

        return validateAndExecute(request, result, context, metadata, generatedSql);
    }

    private AskDataModelResponse generateModelResponse(AskDataRequest request, String prompt, AskDataResult result) {
        if (coreChatModel == null || coreModelRouter == null) {
            AskDataModelResponse response = modelRegistry.generate(request, prompt);
            if (response != null && !isBlank(response.getModelKey())) {
                result.addModelAttempt(response.getModelKey());
                result.setActualModelKey(response.getModelKey());
            }
            return response;
        }
        AskDataModelRequest routeRequest = chatRouteRequest(request);
        AskDataModelResult routed = coreModelRouter.route(routeRequest);
        while (true) {
            result.addModelAttempt(routed.getModelKey());
            publishModelAttempt(request, result, routed, AskDataEvent.ModelState.ATTEMPTED, null, null, null);
            AskDataChatResult chatResult = coreChatModel.chat(new AskDataChatRequest(request.getProjectContext(),
                    prompt, routed.getModelKey(), Collections.<String, Object>emptyMap(), modelTimeoutMillis()));
            if (chatResult == null) {
                publishModelAttempt(request, result, routed, AskDataEvent.ModelState.FAILED,
                        AskDataErrorCode.MODEL_RESPONSE_INVALID, "INVALID_MODEL_RESPONSE", null);
                throw new AskDataModelException(AskDataErrorCode.MODEL_RESPONSE_INVALID,
                        "AskData Core Chat 模型未返回结果");
            }
            if (chatResult.isSuccess()) {
                String actualModelKey = isBlank(chatResult.getActualModelKey())
                        ? routed.getModelKey() : chatResult.getActualModelKey();
                result.setActualModelKey(actualModelKey);
                publishModelAttempt(request, result, routed, AskDataEvent.ModelState.SUCCEEDED,
                        null, null, actualModelKey);
                return new AskDataModelResponse(chatResult.getAnswer(), null, actualModelKey);
            }
            boolean retryableInfrastructureFailure = AskDataChatResult.MODEL_INFRASTRUCTURE_FAILURE
                    .equals(chatResult.getErrorCode()) && chatResult.isRetryable();
            if (!retryableInfrastructureFailure || !properties.getModel().isFallbackEnabled()) {
                publishModelAttempt(request, result, routed, AskDataEvent.ModelState.FAILED,
                        chatResult.getErrorCode(), retryableInfrastructureFailure
                                ? "FALLBACK_DISABLED" : "NON_RETRYABLE_FAILURE",
                        chatResult.getActualModelKey());
                throw new AskDataModelException(chatResult.getErrorCode(), chatResult.getErrorMessage());
            }
            try {
                AskDataModelResult next = coreModelRouter.fallback(routeRequest,
                        AskDataModelResult.infrastructureFailure(routed, chatResult.getErrorMessage()));
                publishModelAttempt(request, result, routed, AskDataEvent.ModelState.FAILED,
                        chatResult.getErrorCode(), "RETRYABLE_INFRASTRUCTURE_FAILURE",
                        chatResult.getActualModelKey());
                routed = next;
            } catch (IllegalArgumentException noCandidate) {
                publishModelAttempt(request, result, routed, AskDataEvent.ModelState.FAILED,
                        chatResult.getErrorCode(), "CANDIDATES_EXHAUSTED", chatResult.getActualModelKey());
                throw new AskDataModelException(chatResult.getErrorCode(), chatResult.getErrorMessage());
            }
        }
    }

    private AskDataModelRequest chatRouteRequest(AskDataRequest request) {
        String[] routeKeys = { "", "", "", "", "" };
        routeKeys[0] = normalized(request == null ? null : request.getModelKey());
        String defaultKey = normalized(properties == null || properties.getModel() == null
                ? null : properties.getModel().getDefaultChatModel());
        int routeIndex = 1;
        if (properties != null && properties.getModel() != null
                && properties.getModel().getChatCandidates() != null) {
            for (String candidate : properties.getModel().getChatCandidates()) {
                String candidateKey = normalized(candidate);
                if (candidateKey.length() > 0 && !candidateKey.equals(routeKeys[0])
                        && !candidateKey.equals(defaultKey) && routeIndex < 4) {
                    routeKeys[routeIndex++] = candidateKey;
                }
            }
        }
        routeKeys[4] = defaultKey;
        return new AskDataModelRequest(request.getProjectContext(), routeKeys[0], routeKeys[1], routeKeys[2],
                routeKeys[3], routeKeys[4]);
    }

    private long modelTimeoutMillis() {
        if (properties == null || properties.getModel() == null
                || properties.getModel().getRequestTimeoutMillis() == null) {
            return 0L;
        }
        return Math.max(0L, properties.getModel().getRequestTimeoutMillis().longValue());
    }

    private long queryTimeoutMillis() {
        Integer timeoutSeconds = properties == null || properties.getSql() == null
                ? null : properties.getSql().getQueryTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds.intValue() <= 0
                ? 0L : timeoutSeconds.longValue() * 1000L;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private AskDataResult resume(AskDataRequest request, AskDataResult result, AskDataStepContext context) {
        if (conversationStore == null) {
            return failure(result, "resume", AskDataErrorCode.CHECKPOINT_NOT_FOUND, "AskData conversation store is not configured");
        }
        AskDataProjectContext projectContext = request.getProjectContext();
        AskDataCheckpoint checkpoint = !isBlank(request.getResumeToken())
                ? conversationStore.loadByResumeToken(projectContext, request.getResumeToken())
                : conversationStore.loadLatest(projectContext, request.getConversationId());
        if (checkpoint == null) {
            return failure(result, "resume", AskDataErrorCode.CHECKPOINT_NOT_FOUND, "AskData checkpoint was not found");
        }
        AskDataCheckpoint latest = conversationStore.loadLatest(projectContext, checkpoint.getConversationId());
        if (latest != null) {
            checkpoint = latest;
        }
        if (!requestIdentityMatches(request, checkpoint, authorizationSummary(context))) {
            return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                    "AskData resume request does not match checkpoint identity");
        }
        if (!equals(checkpointVersion(), checkpoint.getVersion()) || !isSafeCheckpoint(checkpoint)
                || EXECUTION_EXECUTING.equals(checkpoint.getExecutionState())) {
            return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                    "AskData checkpoint does not contain a safe resumable state");
        }
        if (contains(checkpoint, "execute-sql")) {
            AskDataResult governanceFailure = revalidateCompletedCheckpoint(request, result, context, checkpoint);
            if (governanceFailure != null) {
                return governanceFailure;
            }
            restoreCheckpoint(result, context, checkpoint);
            result.addStep("execute-sql", "Execute SQL", true, "SQL execution already completed");
            result.setSuccess(true);
            result.addStep("format-result", "Format Result", true, "Result formatted");
            return result;
        }
        restoreCheckpoint(result, context, checkpoint);
        if (contains(checkpoint, "validate-sql")) {
            return revalidateCheckpointSqlAndExecute(request, result, context, checkpoint);
        }
        if (contains(checkpoint, "generate-sql")) {
            AskDataMetadata metadata;
            try {
                metadata = metadataProvider.load(request, authorizationSummary(context));
            } catch (RuntimeException ex) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
            }
            if (metadata == null) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND,
                        "AskData metadata was not found");
            }
            metadata = authorizedMetadata(request, metadata, authorizationSummary(context));
            if (!hasAuthorizedMetadata(metadata)) {
                return governanceDeniedForMetadata(request, result, authorizationSummary(context));
            }
            metadata = selectTables(request, result, context, metadata,
                    !hasSuccessfulStep(result.getSteps(), AskDataAgentStepKey.SELECT_TABLES));
            if (metadata == null) {
                return result;
            }
            context.setMetadata(metadata);
            return validateAndExecute(request, result, context, metadata, checkpoint.getSql());
        }
        if (contains(checkpoint, "metadata") || contains(checkpoint, "merge-sub-agents")) {
            AskDataMetadata metadata;
            try {
                metadata = metadataProvider.load(request, authorizationSummary(context));
            } catch (RuntimeException ex) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
            }
            if (metadata == null) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND,
                        "AskData metadata was not found");
            }
            metadata = authorizedMetadata(request, metadata, authorizationSummary(context));
            if (!hasAuthorizedMetadata(metadata)) {
                return governanceDeniedForMetadata(request, result, authorizationSummary(context));
            }
            metadata = selectTables(request, result, context, metadata,
                    !hasSuccessfulStep(result.getSteps(), AskDataAgentStepKey.SELECT_TABLES));
            if (metadata == null) {
                return result;
            }
            context.setMetadata(metadata);
            return generateSqlAndValidate(request, result, context, metadata);
        }
        return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                "AskData checkpoint cannot restore the metadata required by the next step");
    }

    private AskDataResult revalidateCompletedCheckpoint(AskDataRequest request, AskDataResult result,
            AskDataStepContext context, AskDataCheckpoint checkpoint) {
        AskDataMetadata metadata;
        try {
            metadata = metadataProvider.load(request, authorizationSummary(context));
        } catch (RuntimeException ex) {
            return governanceDeniedForMetadata(request, result, authorizationSummary(context));
        }
        metadata = authorizedMetadata(request, metadata, authorizationSummary(context));
        if (!hasAuthorizedMetadata(metadata)) {
            return governanceDeniedForMetadata(request, result, authorizationSummary(context));
        }
        context.setMetadata(metadata);
        try {
            AskDataDialect dialect = dialectRegistry.getDialect(checkpoint.getDialect());
            AskDataSqlValidationResult validation = validateCheckpointSql(request, metadata, checkpoint.getSql(), dialect,
                    authorizationSummary(context));
            if (validation == null || !validation.isValid()) {
                return governanceDeniedForMetadata(request, result, authorizationSummary(context));
            }
            context.setSql(validation.getSql());
            context.setSqlValidated(Boolean.TRUE);
        } catch (RuntimeException ex) {
            return governanceDeniedForMetadata(request, result, authorizationSummary(context));
        }
        return null;
    }

    private AskDataResult revalidateCheckpointSqlAndExecute(AskDataRequest request, AskDataResult result,
            AskDataStepContext context, AskDataCheckpoint checkpoint) {
        AskDataMetadata metadata;
        try {
            metadata = metadataProvider.load(request, authorizationSummary(context));
        } catch (RuntimeException ex) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
        }
        if (metadata == null) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND,
                    "AskData metadata was not found");
        }
        metadata = authorizedMetadata(request, metadata, authorizationSummary(context));
        if (!hasAuthorizedMetadata(metadata)) {
                return governanceDeniedForMetadata(request, result, authorizationSummary(context));
        }
        context.setMetadata(metadata);
        AskDataDialect dialect;
        try {
            dialect = dialectRegistry.getDialect(checkpoint.getDialect());
        } catch (RuntimeException ex) {
            return failure(result, "validate-sql", AskDataErrorCode.DIALECT_RESOLUTION_FAILED, message(ex));
        }
        AskDataSqlValidationResult validation;
        try {
            validation = validateCheckpointSql(request, metadata, checkpoint.getSql(), dialect,
                    authorizationSummary(context));
        } catch (RuntimeException ex) {
            return validationFailure(request, result, AskDataSqlValidationResult.rejected(
                    AskDataErrorCode.SQL_REJECTED, message(ex)), authorizationSummary(context));
        }
        if (validation == null || !validation.isValid()) {
            return validationFailure(request, result, validation, authorizationSummary(context));
        }
        context.setSql(validation.getSql());
        context.setSqlValidated(Boolean.TRUE);
        result.setSql(validation.getSql());
        result.addStep("validate-sql", "Validate SQL", true, "SQL revalidated after resume");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
        return checkpointFailure == null ? executeValidatedSql(request, result, context, validation.getCommand())
                : checkpointFailure;
    }

    private AskDataResult validateAndExecute(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataMetadata metadata, String generatedSql) {
        AskDataDialect dialect;
        try {
            dialect = dialectRegistry.getDialect(dialect(request, metadata));
        } catch (RuntimeException ex) {
            return failure(result, "validate-sql", AskDataErrorCode.DIALECT_RESOLUTION_FAILED, message(ex));
        }
        AskDataSqlCommand candidateCommand = candidateCommand(request, generatedSql, authorizationSummary(context));
        com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent validateAgent =
                stepAgent(AskDataAgentStepKey.VALIDATE_SQL);
        if (validateAgent != null) {
            AskDataAgentStepResult agentResult;
            try {
                agentResult = executeStepAgent(validateAgent, agentContext(AskDataAgentStepKey.VALIDATE_SQL,
                        request, result, metadata, context.getPrompt(), candidateCommand, null));
            } catch (RuntimeException ex) {
                return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED, message(ex));
            }
            if (!agentResult.isSuccess()) {
                return agentFailure(result, AskDataAgentStepKey.VALIDATE_SQL, agentResult);
            }
            if (agentResult.getSqlCommand() == null) {
                return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED,
                        "validate-sql Agent 未返回 SQL 命令");
            }
            try {
                candidateCommand = secureAgentCommand(candidateCommand, agentResult.getSqlCommand());
            } catch (RuntimeException ex) {
                return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED, message(ex));
            }
        }
        AskDataSqlValidationResult validation;
        try {
            validation = validateSql(request, metadata, candidateCommand, dialect, authorizationSummary(context));
        } catch (RuntimeException ex) {
            return validationFailure(request, result, AskDataSqlValidationResult.rejected(
                    AskDataErrorCode.SQL_REJECTED, message(ex)), authorizationSummary(context));
        }
        if (validation == null || !validation.isValid()) {
            return validationFailure(request, result, validation, authorizationSummary(context));
        }
        result.setSql(validation.getSql());
        context.setSql(validation.getSql());
        context.setSqlValidated(Boolean.TRUE);
        result.addStep("validate-sql", "Validate SQL", true, "SQL validated");
        publishStepEvent(request, result, AskDataEvent.Type.SQL_VALIDATED, "validate-sql",
                validateAgent == null ? null : validateAgent.getKey(), "AskData SQL 已校验");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
        return checkpointFailure == null ? executeValidatedSql(request, result, context, validation.getCommand())
                : checkpointFailure;
    }

    private AskDataResult executeValidatedSql(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataSqlCommand command) {
        if (command == null) {
            return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED,
                    "AskData SQL validation did not produce a final command");
        }
        AskDataSqlResult structuredResult = null;
        if (isExecutionEnabled(request)) {
            context.setExecutionState(EXECUTION_EXECUTING);
            AskDataResult inFlightFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
            if (inFlightFailure != null) {
                return inFlightFailure;
            }
            publishStepEvent(request, result, AskDataEvent.Type.SQL_EXECUTION_STARTED, "execute-sql", null,
                    "AskData SQL 执行已开始");
            com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent executeAgent =
                    stepAgent(AskDataAgentStepKey.EXECUTE_SQL);
            if (executeAgent == null) {
                AskDataSqlExecutionResult execution;
                publishStepEvent(request, result, AskDataEvent.Type.TOOL_STARTED, "execute-sql",
                        "sql-executor", "AskData SQL 工具调用已开始");
                try {
                    execution = sqlExecutor.execute(request, command);
                } catch (RuntimeException ex) {
                    publishStepEvent(request, result, AskDataEvent.Type.TOOL_FAILED, "execute-sql",
                            "sql-executor", "AskData SQL 工具调用失败");
                    publishStepEvent(request, result, AskDataEvent.Type.SQL_EXECUTION_FAILED, "execute-sql",
                            null, "AskData SQL 执行失败");
                    return failure(result, "execute-sql", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
                }
                if (execution == null || !execution.isSuccess()) {
                    publishStepEvent(request, result, AskDataEvent.Type.TOOL_FAILED, "execute-sql",
                            "sql-executor", "AskData SQL 工具调用失败");
                    publishStepEvent(request, result, AskDataEvent.Type.SQL_EXECUTION_FAILED, "execute-sql",
                            null, "AskData SQL 执行失败");
                    String errorCode = execution == null || isBlank(execution.getErrorCode())
                            ? AskDataErrorCode.SQL_EXECUTION_FAILED : execution.getErrorCode();
                    String errorMessage = execution == null
                            ? "AskData SQL execution failed" : execution.getErrorMessage();
                    return failure(result, "execute-sql", errorCode, errorMessage);
                }
                publishStepEvent(request, result, AskDataEvent.Type.TOOL_COMPLETED, "execute-sql",
                        "sql-executor", "AskData SQL 工具调用已完成");
                structuredResult = toCoreSqlResult(execution, maxRows(request));
            } else {
                AskDataAgentStepResult agentResult;
                try {
                    agentResult = executeStepAgent(executeAgent, agentContext(AskDataAgentStepKey.EXECUTE_SQL,
                            request, result, context.getMetadata(), context.getPrompt(), command, null));
                } catch (RuntimeException ex) {
                    return failure(result, "execute-sql", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
                }
                if (!agentResult.isSuccess()) {
                    return agentFailure(result, AskDataAgentStepKey.EXECUTE_SQL, agentResult);
                }
                structuredResult = agentResult.getSqlResult();
                try {
                    validateAgentSqlResult(structuredResult, maxRows(request));
                } catch (RuntimeException ex) {
                    return failure(result, "execute-sql", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
                }
            }
            result.setRowCount(Integer.valueOf(structuredResult.getExecutedRowCount()));
            context.setRowCount(Integer.valueOf(structuredResult.getExecutedRowCount()));
            context.setSqlExecuted(Boolean.TRUE);
            context.setExecutionState(EXECUTION_COMPLETED);
            result.addStep("execute-sql", "Execute SQL", true, "SQL executed");
            publishStepEvent(request, result, AskDataEvent.Type.SQL_EXECUTED, "execute-sql",
                    executeAgent == null ? null : executeAgent.getKey(), "AskData SQL 已执行");
            AskDataResult checkpointFailure = saveCheckpoint(context, result, "execute-sql",
                    context.getSqlValidated(), context.getSqlExecuted());
            if (checkpointFailure != null) {
                return checkpointFailure;
            }
            try {
                AskDataSqlResult mapped = mapSqlResult(request, command, structuredResult);
                structuredResult = mapped;
                result.setColumns(toLegacyColumns(mapped));
                result.setRows(mapped.getRows());
            } catch (RuntimeException ex) {
                return failure(result, "format-result", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
            }
        } else {
            context.setSqlExecuted(Boolean.FALSE);
            context.setExecutionState(EXECUTION_SKIPPED);
            result.addStep("execute-sql", "Execute SQL", true, "SQL execution skipped");
            publishStepEvent(request, result, AskDataEvent.Type.SQL_EXECUTED, "execute-sql", null,
                    "AskData SQL 执行已跳过");
            AskDataResult checkpointFailure = saveCheckpoint(context, result, "execute-sql",
                    context.getSqlValidated(), context.getSqlExecuted());
            if (checkpointFailure != null) {
                return checkpointFailure;
            }
        }
        AskDataResult interpretationFailure = interpretResult(request, result, context, command, structuredResult);
        if (interpretationFailure != null) {
            return interpretationFailure;
        }
        result.setSuccess(true);
        result.addStep("format-result", "Format Result", true, "Result formatted");
        return result;
    }

    private AskDataResult interpretResult(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataSqlCommand command, AskDataSqlResult sqlResult) {
        com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent interpretAgent =
                stepAgent(AskDataAgentStepKey.INTERPRET_RESULT);
        if (interpretAgent == null) {
            return null;
        }
        AskDataAgentStepResult agentResult;
        try {
            agentResult = executeStepAgent(interpretAgent, agentContext(AskDataAgentStepKey.INTERPRET_RESULT,
                    request, result, context.getMetadata(), context.getPrompt(), command, sqlResult));
        } catch (RuntimeException ex) {
            return failure(result, AskDataAgentStepKey.INTERPRET_RESULT,
                    AskDataErrorCode.SUBQUESTION_FAILED, message(ex));
        }
        if (!agentResult.isSuccess()) {
            return agentFailure(result, AskDataAgentStepKey.INTERPRET_RESULT, agentResult);
        }
        if (isBlank(agentResult.getInterpretation())) {
            return failure(result, AskDataAgentStepKey.INTERPRET_RESULT,
                    AskDataErrorCode.SUBQUESTION_FAILED, "interpret-result Agent 未返回解释");
        }
        result.setBrief(agentResult.getInterpretation());
        result.addStep(AskDataAgentStepKey.INTERPRET_RESULT, "Interpret result", true, "Result interpreted");
        return null;
    }

    private AskDataResult validationFailure(AskDataRequest request, AskDataResult result,
            AskDataSqlValidationResult validation, AskDataAuthorizationSummary authorizationSummary) {
        String errorCode = validation == null || isBlank(validation.getErrorCode()) ? AskDataErrorCode.SQL_REJECTED
                : validation.getErrorCode();
        String errorMessage = validation == null ? "AskData SQL validation failed" : validation.getMessage();
        publishGovernanceDenied(request, result, authorizationSummary, false, errorCode);
        publishStepEvent(request, result, AskDataEvent.Type.SQL_VALIDATION_FAILED, "validate-sql", null,
                "AskData SQL 校验失败");
        return failure(result, "validate-sql", errorCode, errorMessage);
    }

    private AskDataSqlValidationResult validateSql(AskDataRequest request, AskDataMetadata metadata, String sql,
            AskDataDialect dialect) {
        return validateSql(request, metadata, sql, dialect, null);
    }

    private AskDataSqlValidationResult validateSql(AskDataRequest request, AskDataMetadata metadata, String sql,
            AskDataDialect dialect, AskDataAuthorizationSummary authorizationSummary) {
        return validateSql(request, metadata, candidateCommand(request, sql, authorizationSummary), dialect,
                authorizationSummary);
    }

    private AskDataSqlValidationResult validateSql(AskDataRequest request, AskDataMetadata metadata,
            AskDataSqlCommand candidateCommand, AskDataDialect dialect,
            AskDataAuthorizationSummary authorizationSummary) {
        AskDataMetadata authorizedMetadata = authorizedMetadata(request, metadata, authorizationSummary);
        AskDataSqlValidationResult validation = sqlValidator.validate(candidateCommand.getSql(), dialect,
                maxRows(request), authorizedMetadata);
        if (validation == null || !validation.isValid()) {
            return validation;
        }
        AskDataSqlCommand governedCandidate = governedCommand(request, validation.getSql(), authorizationSummary,
                authorizationSummary == null ? Collections.<AskDataRowPredicate>emptyList()
                        : authorizationSummary.getRowPredicates());
        AskDataSqlCommand command = withMergedParameters(candidateCommand.withSql(validation.getSql()),
                governedCandidate);
        if (!command.getSql().equals(validation.getSql())) {
            AskDataSqlValidationResult governedValidation = sqlValidator.validate(command.getSql(), dialect,
                    maxRows(request), authorizedMetadata);
            if (governedValidation == null || !governedValidation.isValid()) {
                return governedValidation;
            }
            command = command.withSql(governedValidation.getSql());
        }
        if (sqlInterceptorChain != null) {
            command = commandForInterceptors(request, command);
            AskDataResult observationResult = observationResult(request);
            publishStepEvent(request, observationResult, AskDataEvent.Type.SQL_PLUGIN_STARTED,
                    "sql-plugin-chain", null, "AskData SQL 插件链已开始");
            AskDataSqlCommand rewritten;
            try {
                rewritten = sqlInterceptorChain.execute(command, sqlAuditCorrelation(request, command));
                publishStepEvent(request, observationResult, AskDataEvent.Type.SQL_PLUGIN_COMPLETED,
                        "sql-plugin-chain", null, "AskData SQL 插件链已完成");
            } catch (RuntimeException ex) {
                publishStepEvent(request, observationResult, AskDataEvent.Type.SQL_PLUGIN_FAILED,
                        "sql-plugin-chain", null, "AskData SQL 插件链失败");
                throw ex;
            }
            AskDataAuthorizationSummary finalSummary = rewritten.getAuthorizationSummary();
            AskDataSqlCommand governed = governedCommand(request, rewritten.getSql(), finalSummary,
                    finalSummary == null ? Collections.<AskDataRowPredicate>emptyList()
                            : finalSummary.getRowPredicates());
            command = withMergedParameters(rewritten, governed);
            AskDataMetadata finalMetadata = authorizedMetadata(request, metadata, command.getAuthorizationSummary());
            AskDataSqlValidationResult finalValidation = sqlValidator.validate(command.getSql(), dialect,
                    maxRows(request), finalMetadata);
            if (finalValidation == null || !finalValidation.isValid()) {
                return finalValidation;
            }
            command = command.withSql(finalValidation.getSql());
        }
        validateNamedParameters(command);
        validateCoreCommand(command);
        return AskDataSqlValidationResult.accepted(command);
    }

    private AskDataSqlCommand candidateCommand(AskDataRequest request, String sql,
            AskDataAuthorizationSummary authorizationSummary) {
        return new AskDataSqlCommand(request.getProjectContext(), sql, sqlParameters(request, authorizationSummary, sql),
                authorizedObjects(request, authorizationSummary), authorizationSummary);
    }

    private AskDataSqlCommand secureAgentCommand(AskDataSqlCommand original, AskDataSqlCommand rewritten) {
        if (!sameProjectContext(original.getContext(), rewritten.getContext())
                || original.getAuthorizationSummary() != rewritten.getAuthorizationSummary()
                || !original.getAuthorizedObjects().equals(rewritten.getAuthorizedObjects())) {
            throw new IllegalArgumentException("Agent 不能替换项目、操作人或治理授权边界");
        }
        List<AskDataSqlParameter> parameters = new ArrayList<AskDataSqlParameter>(original.getParameters());
        for (AskDataSqlParameter required : original.getParameters()) {
            AskDataSqlParameter candidate = rewritten.getParameter(required.getName());
            if (candidate == null || !required.hasSameValue(candidate)) {
                throw new IllegalArgumentException("Agent 不能删除或修改命名参数: " + required.getName());
            }
        }
        for (AskDataSqlParameter parameter : rewritten.getParameters()) {
            AskDataSqlParameter existing = original.getParameter(parameter.getName());
            if (existing == null) {
                parameters.add(parameter);
            } else if (!existing.hasSameValue(parameter)) {
                throw new IllegalArgumentException("命名参数冲突: " + parameter.getName());
            }
        }
        return new AskDataSqlCommand(original.getContext(), rewritten.getSql(), parameters,
                original.getAuthorizedObjects(), original.getAuthorizationSummary());
    }

    private boolean sameProjectContext(AskDataProjectContext first, AskDataProjectContext second) {
        return first != null && second != null && equals(first.getProjectId(), second.getProjectId())
                && equals(first.getOperatorId(), second.getOperatorId());
    }

    private AskDataSqlCommand withMergedParameters(AskDataSqlCommand pluginCommand, AskDataSqlCommand governedCommand) {
        Map<String, AskDataSqlParameter> parameters = new LinkedHashMap<String, AskDataSqlParameter>();
        for (AskDataSqlParameter parameter : pluginCommand.getParameters()) {
            parameters.put(parameter.getName(), parameter);
        }
        for (AskDataSqlParameter parameter : governedCommand.getParameters()) {
            AskDataSqlParameter existing = parameters.get(parameter.getName());
            if (existing != null && !existing.hasSameValue(parameter)) {
                throw new IllegalArgumentException("命名参数冲突: " + parameter.getName());
            }
            if (existing == null) {
                parameters.put(parameter.getName(), parameter);
            }
        }
        return new AskDataSqlCommand(governedCommand.getContext(), governedCommand.getSql(),
                new ArrayList<AskDataSqlParameter>(parameters.values()), governedCommand.getAuthorizedObjects(),
                governedCommand.getAuthorizationSummary());
    }

    private void validateNamedParameters(AskDataSqlCommand command) {
        for (String parameterName : namedParameterNames(command.getSql())) {
            if (command.getParameter(parameterName) == null) {
                throw new IllegalArgumentException("无法绑定命名参数: " + parameterName);
            }
        }
    }

    private AskDataSqlResult toCoreSqlResult(AskDataSqlExecutionResult execution, int rowLimit) {
        List<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlColumn> columns =
                new ArrayList<com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlColumn>();
        for (AskDataColumn column : execution.getColumns()) {
            columns.add(new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlColumn(column.getName(),
                    column.getLabel(), column.getType()));
        }
        return AskDataSqlResult.of(columns, execution.getRows(), AskDataSqlExecutionStatus.SUCCEEDED,
                execution.getRowCount(), rowLimit, queryTimeoutMillis());
    }

    private AskDataSqlResult mapSqlResult(AskDataRequest request, AskDataSqlCommand command, AskDataSqlResult result) {
        return sqlInterceptorChain == null ? result
                : sqlInterceptorChain.mapResult(command, result, sqlAuditCorrelation(request, command));
    }

    private com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit.Correlation sqlAuditCorrelation(
            AskDataRequest request, AskDataSqlCommand command) {
        String conversationId = request == null || isBlank(request.getConversationId())
                ? "unassigned" : request.getConversationId();
        String requestId = observationOption(request, OBSERVABILITY_REQUEST_ID_OPTION);
        if (isBlank(requestId)) { requestId = conversationId; }
        AskDataAuthorizationSummary summary = command == null ? null : command.getAuthorizationSummary();
        String authorizationVersion = summary == null || isBlank(summary.getAuthorizationVersion())
                ? GOVERNANCE_ERROR_VERSION : summary.getAuthorizationVersion();
        return new com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlAudit.Correlation(
                requestId, conversationId, authorizationVersion);
    }

    private void validateAgentSqlResult(AskDataSqlResult result, int rowLimit) {
        if (result == null) {
            throw new IllegalArgumentException("execute-sql Agent 未返回 SQL 结果");
        }
        if (result.getExecutionStatus() != AskDataSqlExecutionStatus.SUCCEEDED) {
            throw new IllegalArgumentException("execute-sql Agent 返回非成功执行状态: "
                    + result.getExecutionStatus());
        }
        if (result.getRowLimit() != rowLimit || result.getTimeoutMillis() != queryTimeoutMillis()) {
            throw new IllegalArgumentException("execute-sql Agent 不能改变行数上限或超时边界");
        }
        int maximumRows = Math.min(result.getExecutedRowCount(), rowLimit);
        if (result.getExecutedRowCount() > rowLimit || result.getRows().size() > maximumRows) {
            throw new IllegalArgumentException("execute-sql Agent 返回结果超过受限行数");
        }
    }

    private List<AskDataColumn> toLegacyColumns(AskDataSqlResult result) {
        List<AskDataColumn> columns = new ArrayList<AskDataColumn>();
        for (com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlColumn mappedColumn : result.getColumns()) {
            AskDataColumn column = new AskDataColumn();
            column.setName(mappedColumn.getName());
            column.setLabel(mappedColumn.getLabel());
            column.setType(mappedColumn.getType());
            columns.add(column);
        }
        return columns;
    }

    private AskDataSqlValidationResult validateCheckpointSql(AskDataRequest request, AskDataMetadata metadata,
            String sql, AskDataDialect dialect, AskDataAuthorizationSummary authorizationSummary) {
        AskDataMetadata authorizedMetadata = authorizedMetadata(request, metadata, authorizationSummary);
        AskDataSqlValidationResult validation = sqlValidator.validate(sql, dialect, maxRows(request), authorizedMetadata);
        if (validation == null || !validation.isValid()) {
            return validation;
        }
        AskDataSqlCommand command = governedCommand(request, validation.getSql(), authorizationSummary,
                authorizationSummary == null ? Collections.<AskDataRowPredicate>emptyList()
                        : authorizationSummary.getRowPredicates());
        if (!command.getSql().equals(validation.getSql())) {
            AskDataSqlValidationResult finalValidation = sqlValidator.validate(command.getSql(), dialect,
                    maxRows(request), authorizedMetadata);
            if (finalValidation == null || !finalValidation.isValid()) {
                return finalValidation;
            }
            command = command.withSql(finalValidation.getSql());
        }
        validateCoreCommand(command);
        return AskDataSqlValidationResult.accepted(command);
    }

    private AskDataSqlCommand governedCommand(AskDataRequest request, String sql,
            AskDataAuthorizationSummary authorizationSummary, List<AskDataRowPredicate> predicatesToInject) {
        String governedSql = AskDataSqlAuthorizationProcessor.injectRowPredicates(sql, request.getDatasourceKey(),
                predicatesToInject);
        return new AskDataSqlCommand(request.getProjectContext(), governedSql,
                sqlParameters(request, authorizationSummary, governedSql),
                authorizedObjects(request, authorizationSummary),
                authorizationSummary);
    }

    private AskDataSqlCommand commandForInterceptors(AskDataRequest request, AskDataSqlCommand command) {
        if (request == null || request.getOptions() == null || request.getOptions().isEmpty()) {
            return command;
        }
        List<AskDataSqlParameter> parameters = new ArrayList<AskDataSqlParameter>(command.getParameters());
        for (Map.Entry<String, Object> entry : new TreeMap<String, Object>(request.getOptions()).entrySet()) {
            if (command.getParameter(entry.getKey()) == null) {
                parameters.add(AskDataSqlParameter.of(entry.getKey(), entry.getValue()));
            }
        }
        return new AskDataSqlCommand(command.getContext(), command.getSql(), parameters,
                command.getAuthorizedObjects(), command.getAuthorizationSummary());
    }

    private void validateCoreCommand(AskDataSqlCommand command) {
        if (coreSqlValidator == null) {
            return;
        }
        com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator.Result result = coreSqlValidator.validate(command);
        if (result == null) {
            throw new IllegalArgumentException("SQL 校验器不能返回 null");
        }
        if (!result.isAccepted()) {
            throw new IllegalArgumentException(result.getFailureCategory().name() + ": " + result.getMessage());
        }
    }

    private List<AskDataSqlParameter> sqlParameters(AskDataRequest request,
            AskDataAuthorizationSummary authorizationSummary, String sql) {
        Map<String, AskDataSqlParameter> parameters = new LinkedHashMap<String, AskDataSqlParameter>();
        Set<String> referencedNames = namedParameterNames(sql);
        if (request != null && request.getOptions() != null) {
            for (Map.Entry<String, Object> entry : new TreeMap<String, Object>(request.getOptions()).entrySet()) {
                if (referencedNames.contains(entry.getKey())) {
                    addSqlParameter(parameters, AskDataSqlParameter.of(entry.getKey(), entry.getValue()));
                }
            }
        }
        if (authorizationSummary != null) {
            for (AskDataRowPredicate predicate : authorizationSummary.getRowPredicates()) {
                if (predicate == null || !equals(request.getDatasourceKey(),
                        predicate.getAuthorizedObject().getDatasourceKey())) {
                    continue;
                }
                for (AskDataSqlParameter parameter : predicate.getParameters()) {
                    if (referencedNames.contains(parameter.getName())) {
                        addSqlParameter(parameters, parameter);
                    }
                }
            }
        }
        return new ArrayList<AskDataSqlParameter>(parameters.values());
    }

    private Set<String> namedParameterNames(String sql) {
        Set<String> names = new LinkedHashSet<String>();
        if (sql == null) {
            return names;
        }
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'' || current == '"' || current == '`') {
                index = skipQuotedSql(sql, index, current) - 1;
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = skipLineCommentSql(sql, index + 2) - 1;
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockCommentSql(sql, index + 2) - 1;
            } else if (current == ':' && (index == 0 || sql.charAt(index - 1) != ':')
                    && index + 1 < sql.length() && Character.isJavaIdentifierStart(sql.charAt(index + 1))) {
                int end = index + 2;
                while (end < sql.length() && Character.isJavaIdentifierPart(sql.charAt(end))) {
                    end++;
                }
                names.add(sql.substring(index + 1, end));
                index = end - 1;
            }
        }
        return names;
    }

    private int skipQuotedSql(String sql, int index, char quote) {
        int cursor = index + 1;
        while (cursor < sql.length()) {
            if (sql.charAt(cursor) == quote) {
                if (cursor + 1 < sql.length() && sql.charAt(cursor + 1) == quote) {
                    cursor += 2;
                    continue;
                }
                return cursor + 1;
            }
            cursor++;
        }
        return cursor;
    }

    private int skipLineCommentSql(String sql, int index) {
        while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private int skipBlockCommentSql(String sql, int index) {
        while (index + 1 < sql.length()) {
            if (sql.charAt(index) == '*' && sql.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return sql.length();
    }

    private void addSqlParameter(Map<String, AskDataSqlParameter> parameters, AskDataSqlParameter parameter) {
        if (parameters.put(parameter.getName(), parameter) != null) {
            throw new IllegalArgumentException("命名参数冲突: " + parameter.getName());
        }
    }

    private Set<String> authorizedObjects(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary) {
        Set<String> authorizedObjects = new LinkedHashSet<String>();
        if (request == null || authorizationSummary == null || authorizationSummary.getAuthorizedObjects() == null) {
            return authorizedObjects;
        }
        for (AskDataAuthorizedObject authorizedObject : authorizationSummary.getAuthorizedObjects()) {
            if (authorizedObject != null && equals(request.getDatasourceKey(), authorizedObject.getDatasourceKey())) {
                authorizedObjects.add(authorizedObject.getTableName());
            }
        }
        return authorizedObjects;
    }

    private AskDataMetadata authorizedMetadata(AskDataRequest request, AskDataMetadata metadata,
            AskDataAuthorizationSummary authorizationSummary) {
        if (metadata == null) {
            return null;
        }
        List<AskDataTable> tables = new ArrayList<AskDataTable>();
        for (AskDataTable table : metadata.getTables()) {
            if (table == null) {
                continue;
            }
            List<com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn> columns =
                    new ArrayList<com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn>();
            boolean tableAuthorized = false;
            boolean tableLevelAuthorized = false;
            for (AskDataAuthorizedObject authorizedObject : authorizationSummary == null
                    ? Collections.<AskDataAuthorizedObject>emptyList()
                    : authorizationSummary.getAuthorizedObjects()) {
                if (authorizedObject == null || request == null
                        || !equals(request.getDatasourceKey(), authorizedObject.getDatasourceKey())
                        || !equals(table.getName(), authorizedObject.getTableName())) {
                    continue;
                }
                tableAuthorized = true;
                tableLevelAuthorized = tableLevelAuthorized || "*".equals(authorizedObject.getColumnName());
                for (com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn column
                        : table.getColumns()) {
                    if (column != null && ("*".equals(authorizedObject.getColumnName())
                            || equals(column.getName(), authorizedObject.getColumnName()))
                            && !columns.contains(column)) {
                        columns.add(column);
                    }
                }
            }
            if (tableAuthorized) {
                tables.add(new AskDataTable(table.getName(), columns, tableLevelAuthorized));
            }
        }
        return new AskDataMetadata(metadata.getDialect(), tables, Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    private boolean hasAuthorizedMetadata(AskDataMetadata metadata) {
        return metadata != null && !metadata.getTables().isEmpty();
    }

    private AskDataResult governanceDeniedForMetadata(AskDataRequest request, AskDataResult result,
            AskDataAuthorizationSummary authorizationSummary) {
        publishGovernanceDenied(request, result, authorizationSummary, false);
        return failure(result, "authorize", DefaultDenyAskDataGovernanceProvider.DENIAL_CODE,
                "AskData 请求未获授权");
    }

    private AskDataAuthorizationSummary authorize(AskDataRequest request, AskDataResult result) {
        if (governanceProvider == null) {
            return null;
        }
        AskDataAuthorizationSummary authorizationSummary = null;
        try {
            AskDataAuthorizationRequest authorizationRequest = new AskDataAuthorizationRequest(
                    request.getProjectContext(), request.getDatasourceKey(), AskDataAuthorizationPurpose.QUERY,
                    AskDataAdministrativeScope.none());
            authorizationSummary = governanceProvider.authorize(authorizationRequest);
            if (authorizationSummary != null && authorizationSummary.isAllowed()
                    && !authorizedObjects(request, authorizationSummary).isEmpty()
                    && authorizationSummary.isBoundTo(authorizationRequest)) {
                return authorizationSummary;
            }
        } catch (RuntimeException ex) {
            publishGovernanceDenied(request, result, null, true);
            failure(result, "authorize", DefaultDenyAskDataGovernanceProvider.DENIAL_CODE,
                    "AskData 请求未获授权");
            return null;
        }
        publishGovernanceDenied(request, result, authorizationSummary, false);
        String denialCode = authorizationSummary == null || isBlank(authorizationSummary.getDenialCode())
                ? DefaultDenyAskDataGovernanceProvider.DENIAL_CODE : authorizationSummary.getDenialCode();
        failure(result, "authorize", denialCode,
                "AskData 请求未获授权");
        return null;
    }

    private void publishGovernanceDenied(AskDataRequest request, AskDataResult result,
            AskDataAuthorizationSummary authorizationSummary, boolean providerError) {
        publishGovernanceDenied(request, result, authorizationSummary, providerError, null);
    }

    private void publishGovernanceDenied(AskDataRequest request, AskDataResult result,
            AskDataAuthorizationSummary authorizationSummary, boolean providerError, String denialCodeOverride) {
        if (eventPublisher == null || request == null || request.getProjectContext() == null) {
            return;
        }
        String denialCode = !isBlank(denialCodeOverride) ? denialCodeOverride
                : authorizationSummary == null || isBlank(authorizationSummary.getDenialCode())
                        ? DefaultDenyAskDataGovernanceProvider.DENIAL_CODE : authorizationSummary.getDenialCode();
        String version = providerError || authorizationSummary == null
                ? GOVERNANCE_ERROR_VERSION : authorizationSummary.getAuthorizationVersion();
        String source = providerError || authorizationSummary == null
                ? GOVERNANCE_ERROR_SOURCE : authorizationSummary.getAuthorizationSource();
        String traceId = result == null || isBlank(result.getConversationId())
                ? UUID.randomUUID().toString() : result.getConversationId();
        safePublish(AskDataEvent.governanceDenied(request.getProjectContext(), traceId,
                request.getDatasourceKey(), denialCode, version, source));
    }

    private AskDataAuthorizationSummary authorizationSummary(AskDataStepContext context) {
        Object value = context.getAttributes().get(AUTHORIZATION_SUMMARY_ATTRIBUTE);
        return value instanceof AskDataAuthorizationSummary ? (AskDataAuthorizationSummary) value : null;
    }

    private AskDataResult saveCheckpoint(AskDataStepContext context, AskDataResult result, String currentStep,
            Boolean sqlValidated, Boolean sqlExecuted) {
        if (conversationStore == null || contextCompressor == null || isBlank(result.getConversationId())) {
            return null;
        }
        boolean compressionCompleted = false;
        try {
            context.setCompletedSteps(completedSteps(result));
            context.setStepLogs(result.getSteps());
            context.setSqlValidated(sqlValidated == null ? context.getSqlValidated() : sqlValidated);
            context.setSqlExecuted(sqlExecuted == null ? context.getSqlExecuted() : sqlExecuted);
            context.setRowCount(result.getRowCount());
            context.setCurrentStep(currentStep);
            context.setConversationId(result.getConversationId());
            context.setCheckpointId(UUID.randomUUID().toString());
            context.setResumeToken(UUID.randomUUID().toString());
            context.setActualModelKey(result.getActualModelKey());
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.CONTEXT_COMPRESSION_STARTED,
                    currentStep, null, "AskData 上下文压缩已开始");
            AskDataCompressedContext compressed = contextCompressor.compress(context);
            compressionCompleted = true;
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.CONTEXT_COMPRESSION_COMPLETED,
                    currentStep, null, "AskData 上下文压缩已完成");
            AskDataCheckpoint checkpoint = new AskDataCheckpoint();
            AskDataRequest request = context.getRequest();
            AskDataProjectContext projectContext = request == null ? null : request.getProjectContext();
            AskDataAuthorizationSummary authorizationSummary = authorizationSummary(context);
            checkpoint.setConversationId(result.getConversationId());
            checkpoint.setProjectId(projectContext == null ? null : projectContext.getProjectId());
            checkpoint.setOperatorId(projectContext == null ? null : projectContext.getOperatorId());
            checkpoint.setAuthorizationVersion(authorizationSummary == null
                    ? null : authorizationSummary.getAuthorizationVersion());
            checkpoint.setCheckpointId(context.getCheckpointId());
            checkpoint.setResumeToken(context.getResumeToken());
            checkpoint.setRequestSummary(request == null ? null : request.getQuestion());
            checkpoint.setModelKey(request == null ? null : request.getModelKey());
            checkpoint.setActualModelKey(result.getActualModelKey());
            checkpoint.setModelAttempts(result.getModelAttempts());
            checkpoint.setActualEmbeddingModelKey(result.getActualEmbeddingModelKey());
            checkpoint.setEmbeddingModelAttempts(result.getEmbeddingModelAttempts());
            checkpoint.setDatasourceKey(request == null ? null : request.getDatasourceKey());
            checkpoint.setMaxRows(Integer.valueOf(maxRows(request)));
            checkpoint.setExecute(Boolean.valueOf(isExecutionEnabled(request)));
            checkpoint.setRequestOptionsSnapshot(checkpointScopeSnapshot(request));
            checkpoint.setDialect(request != null && !isBlank(request.getDialect()) ? request.getDialect()
                    : context.getMetadata() == null ? null : context.getMetadata().getDialect());
            checkpoint.setCompletedSteps(context.getCompletedSteps());
            checkpoint.setCurrentStep(currentStep);
            checkpoint.setSql(context.getSql());
            checkpoint.setSqlValidated(context.getSqlValidated());
            checkpoint.setSqlExecuted(context.getSqlExecuted());
            checkpoint.setExecutionState(context.getExecutionState());
            checkpoint.setRowCount(context.getRowCount());
            checkpoint.setCompressedSummary(compressed == null ? null : compressed.getCompressedSummary());
            checkpoint.setSteps(context.getStepLogs());
            checkpoint.setErrorCode(result.getErrorCode());
            checkpoint.setVersion(checkpointVersion());
            checkpoint.setCreatedAt(new Date());
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.CHECKPOINT_SAVE_STARTED,
                    currentStep, null, "AskData 检查点保存已开始");
            AskDataCheckpoint saved = conversationStore.save(projectContext, checkpoint);
            AskDataCheckpoint responseCheckpoint = saved == null ? checkpoint : saved;
            result.setCheckpointId(responseCheckpoint.getCheckpointId());
            result.setResumeToken(responseCheckpoint.getResumeToken());
            publishStepEvent(context.getRequest(), result, AskDataEvent.Type.CHECKPOINT_SAVED, currentStep, null,
                    "AskData 检查点已保存");
            return null;
        } catch (RuntimeException ex) {
            publishStepEvent(context.getRequest(), result, compressionCompleted
                            ? AskDataEvent.Type.CHECKPOINT_SAVE_FAILED
                            : AskDataEvent.Type.CONTEXT_COMPRESSION_FAILED,
                    currentStep, null, compressionCompleted ? "AskData 检查点保存失败" : "AskData 上下文压缩失败");
            return failure(result, currentStep, AskDataErrorCode.CONTEXT_COMPRESSION_FAILED, message(ex));
        }
    }

    private void restoreResult(AskDataResult result, AskDataCheckpoint checkpoint) {
        result.setConversationId(checkpoint.getConversationId());
        result.setCheckpointId(checkpoint.getCheckpointId());
        result.setResumeToken(checkpoint.getResumeToken());
        result.setSql(checkpoint.getSql());
        result.setRowCount(checkpoint.getRowCount());
        result.setActualModelKey(checkpoint.getActualModelKey());
        result.setModelAttempts(checkpoint.getModelAttempts());
        result.setActualEmbeddingModelKey(checkpoint.getActualEmbeddingModelKey());
        result.setEmbeddingModelAttempts(checkpoint.getEmbeddingModelAttempts());
        result.setSteps(new ArrayList<AskDataStepLog>(checkpoint.getSteps()));
        for (String completedStep : checkpoint.getCompletedSteps()) {
            if (!hasSuccessfulStep(result.getSteps(), completedStep)) {
                result.addStep(completedStep, completedStep, true, "Checkpoint restored");
            }
        }
    }

    private void restoreCheckpoint(AskDataResult result, AskDataStepContext context, AskDataCheckpoint checkpoint) {
        restoreResult(result, checkpoint);
        context.setSql(checkpoint.getSql());
        context.setSqlValidated(checkpoint.getSqlValidated());
        context.setSqlExecuted(checkpoint.getSqlExecuted());
        context.setExecutionState(checkpoint.getExecutionState());
        context.setRowCount(checkpoint.getRowCount());
        context.setCurrentStep(checkpoint.getCurrentStep());
        context.setConversationId(checkpoint.getConversationId());
        context.setCheckpointId(checkpoint.getCheckpointId());
        context.setResumeToken(checkpoint.getResumeToken());
        context.setActualModelKey(checkpoint.getActualModelKey());
        context.setCompletedSteps(checkpoint.getCompletedSteps());
        context.setStepLogs(checkpoint.getSteps());
        context.setErrorCode(checkpoint.getErrorCode());
    }

    private boolean isSafeCheckpoint(AskDataCheckpoint checkpoint) {
        List<String> completed = checkpoint.getCompletedSteps();
        if (completed == null || completed.isEmpty()) {
            return false;
        }
        List<String> expected = completed.contains("merge-sub-agents") ? AGENT_CHECKPOINT_STEPS : CHECKPOINT_STEPS;
        if (completed.size() > expected.size()) {
            return false;
        }
        for (int index = 0; index < completed.size(); index++) {
            if (!equals(expected.get(index), completed.get(index))) {
                return false;
            }
        }
        String currentStep = completed.get(completed.size() - 1);
        if (!equals(currentStep, checkpoint.getCurrentStep())) {
            return false;
        }
        if ("metadata".equals(currentStep)) {
            return isBlank(checkpoint.getSql()) && checkpoint.getSqlValidated() == null
                    && checkpoint.getSqlExecuted() == null && checkpoint.getRowCount() == null;
        }
        if ("merge-sub-agents".equals(currentStep)) {
            return isBlank(checkpoint.getSql()) && checkpoint.getSqlValidated() == null
                    && checkpoint.getSqlExecuted() == null && checkpoint.getRowCount() == null
                    && checkpoint.getExecutionState() == null;
        }
        if ("prompt".equals(currentStep)) {
            return false;
        }
        if (isBlank(checkpoint.getSql()) || isBlank(checkpoint.getDialect())) {
            return false;
        }
        if ("generate-sql".equals(currentStep)) {
            return checkpoint.getSqlValidated() == null && checkpoint.getSqlExecuted() == null
                    && checkpoint.getRowCount() == null;
        }
        if ("validate-sql".equals(currentStep)) {
            return Boolean.TRUE.equals(checkpoint.getSqlValidated()) && checkpoint.getSqlExecuted() == null
                    && checkpoint.getRowCount() == null && (checkpoint.getExecutionState() == null
                    || EXECUTION_EXECUTING.equals(checkpoint.getExecutionState()));
        }
        if (!Boolean.TRUE.equals(checkpoint.getSqlValidated()) || checkpoint.getSqlExecuted() == null) {
            return false;
        }
        return Boolean.TRUE.equals(checkpoint.getSqlExecuted())
                ? checkpoint.getRowCount() != null && (checkpoint.getExecutionState() == null
                || EXECUTION_COMPLETED.equals(checkpoint.getExecutionState()))
                : checkpoint.getRowCount() == null && (checkpoint.getExecutionState() == null
                || EXECUTION_SKIPPED.equals(checkpoint.getExecutionState()));
    }

    private boolean hasSuccessfulStep(List<AskDataStepLog> steps, String stepKey) {
        for (AskDataStepLog step : steps) {
            if (step.isSuccess() && equals(stepKey, step.getStepKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(AskDataCheckpoint checkpoint, String step) {
        return checkpoint.getCompletedSteps() != null && checkpoint.getCompletedSteps().contains(step);
    }

    private boolean isResumeRequested(AskDataRequest request) {
        return request != null && (!isBlank(request.getResumeToken()) || !isBlank(request.getConversationId()));
    }

    private List<String> completedSteps(AskDataResult result) {
        List<String> completed = new ArrayList<String>();
        for (AskDataStepLog step : result.getSteps()) {
            if (step.isSuccess() && (CHECKPOINT_STEPS.contains(step.getStepKey())
                    || "merge-sub-agents".equals(step.getStepKey()))) {
                completed.add(step.getStepKey());
            }
        }
        return completed;
    }

    private AskDataResult failure(AskDataResult result, String stepKey, String errorCode, String errorMessage) {
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.addStep(stepKey, stepKey, false, errorMessage);
        return result;
    }

    private void publishLifecycleEvent(AskDataRequest request, AskDataResult result, AskDataEvent.Type type,
            String message) {
        saveAgentState(request, result, eventStatus(type), type == null ? "lifecycle" : type.name());
        if (eventPublisher == null || request == null) {
            return;
        }
        AskDataProjectContext projectContext = request.getProjectContext();
        if (projectContext != null) {
            safePublish(correlatedEvent(request, result, type, message, null, null));
        }
    }

    private void publishStepEvent(AskDataRequest request, AskDataResult result, AskDataEvent.Type type,
            String graphNodeId, String agentKey, String message) {
        saveAgentState(request, result, eventStatus(type), graphNodeId);
        if (eventPublisher == null || request == null || request.getProjectContext() == null || result == null) {
            return;
        }
        safePublish(correlatedEvent(request, result, type, message, graphNodeId, agentKey));
    }

    private AskDataEvent correlatedEvent(AskDataRequest request, AskDataResult result, AskDataEvent.Type type,
            String message, String graphNodeId, String agentKey) {
        AskDataEventCorrelation correlation = eventCorrelation(request, result, graphNodeId, agentKey, type);
        return AskDataEvent.correlated(request.getProjectContext(), correlation.getConversationId(), type, message,
                correlation);
    }

    private AskDataEventCorrelation eventCorrelation(AskDataRequest request, AskDataResult result,
            String graphNodeId, String agentKey) {
        return eventCorrelation(request, result, graphNodeId, agentKey, null);
    }

    private AskDataEventCorrelation eventCorrelation(AskDataRequest request, AskDataResult result,
            String graphNodeId, String agentKey, AskDataEvent.Type type) {
        String conversationId = !isBlank(result.getConversationId()) ? result.getConversationId()
                : !isBlank(request.getConversationId()) ? request.getConversationId() : "unassigned";
        String requestId = observationOption(request, OBSERVABILITY_REQUEST_ID_OPTION);
        if (isBlank(requestId)) { requestId = conversationId; }
        String subQuestionId = !isBlank(result.getQuestionId()) ? result.getQuestionId()
                : observationOption(request, OBSERVABILITY_SUB_QUESTION_ID_OPTION);
        String rootEventId = requestId + ":root";
        AskDataEventCorrelation.Builder builder = AskDataEventCorrelation.builder(requestId, conversationId)
                .subQuestionId(subQuestionId).graphNodeId(graphNodeId).agentKey(agentKey)
                .instanceId(instanceId());
        if (type == AskDataEvent.Type.CONVERSATION_STARTED) {
            builder.eventId(rootEventId);
        } else {
            builder.parentEventId(rootEventId);
        }
        return builder.build();
    }

    private void publishSubQuestionEvent(AskDataRequest request, AskDataResult parent, String subQuestionId,
            AskDataEvent.Type type, String message) {
        if (eventPublisher == null || request == null || request.getProjectContext() == null || parent == null) {
            return;
        }
        String conversationId = isBlank(parent.getConversationId()) ? "unassigned" : parent.getConversationId();
        String requestId = observationOption(request, OBSERVABILITY_REQUEST_ID_OPTION);
        if (isBlank(requestId)) { requestId = conversationId; }
        AskDataEventCorrelation correlation = AskDataEventCorrelation.builder(requestId, conversationId)
                .subQuestionId(subQuestionId).graphNodeId("multi-question").instanceId(instanceId())
                .parentEventId(requestId + ":root").build();
        safePublish(AskDataEvent.correlated(request.getProjectContext(), conversationId, type, message, correlation));
    }

    private void publishAgentStepEvent(com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgent agent,
            AskDataAgentStepContext context, AskDataEvent.Type type) {
        if (eventPublisher == null || agent == null || context == null || context.getProjectContext() == null) {
            return;
        }
        String conversationId = isBlank(context.getConversationId()) ? "unassigned" : context.getConversationId();
        AskDataEventCorrelation correlation = AskDataEventCorrelation.builder(conversationId, conversationId)
                .graphNodeId(context.getStepKey()).agentKey(agent.getKey()).instanceId(instanceId())
                .parentEventId(conversationId + ":root").build();
        safePublish(AskDataEvent.correlated(context.getProjectContext(), conversationId, type,
                type == AskDataEvent.Type.AGENT_NODE_EXECUTED ? "AskData Agent 节点已完成" : "AskData Agent 节点失败",
                correlation));
    }

    private AskDataEvent.Type subQuestionEventType(AskDataSubQuestionResult.State state) {
        if (state == AskDataSubQuestionResult.State.SUCCEEDED) { return AskDataEvent.Type.SUB_QUESTION_COMPLETED; }
        if (state == AskDataSubQuestionResult.State.TIMED_OUT) { return AskDataEvent.Type.SUB_QUESTION_TIMED_OUT; }
        if (state == AskDataSubQuestionResult.State.CANCELLED) { return AskDataEvent.Type.SUB_QUESTION_CANCELLED; }
        if (state == AskDataSubQuestionResult.State.SKIPPED) { return AskDataEvent.Type.SUB_QUESTION_SKIPPED; }
        return AskDataEvent.Type.SUB_QUESTION_FAILED;
    }

    private String observationOption(AskDataRequest request, String key) {
        if (request == null || request.getOptions() == null) { return null; }
        Object value = request.getOptions().get(key);
        return value instanceof String && !isBlank((String) value) ? ((String) value).trim() : null;
    }

    private String instanceId() {
        String configured = System.getProperty("fw.plus.ai.askdata.instance-id");
        return isBlank(configured) ? "local" : configured.trim();
    }

    private void safePublish(AskDataEvent event) {
        if (eventPublisher == null || event == null) { return; }
        try { eventPublisher.publish(event); } catch (RuntimeException ignored) {
            // 可观测性失败不能改变模型、SQL、检查点或对话状态。
        }
    }

    private void publishModelAttempt(AskDataRequest request, AskDataResult result, AskDataModelResult routed,
            AskDataEvent.ModelState state, String failureCode, String switchReason, String actualModelKey) {
        if (eventPublisher == null || request == null || request.getProjectContext() == null) {
            return;
        }
        safePublish(AskDataEvent.modelAttempt(request.getProjectContext(), result.getConversationId(),
                AskDataEvent.ModelType.CHAT, routed.getModelKey(), routed.getRoutePolicy(), state, failureCode,
                switchReason, actualModelKey, eventCorrelation(request, result, "model", null)));
    }

    private boolean hasValidProjectContext(AskDataRequest request) {
        if (request == null || request.getProjectContext() == null) {
            return false;
        }
        AskDataProjectContext projectContext = request.getProjectContext();
        return !isBlankProjectScopeIdentifier(projectContext.getProjectId())
                && !isBlankProjectScopeIdentifier(projectContext.getOperatorId());
    }

    private boolean isBlankProjectScopeIdentifier(String value) {
        if (value == null || value.length() == 0) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)) {
                return false;
            }
        }
        return true;
    }

    private boolean recordIdempotency(AskDataRequest request, AskDataResult result) {
        if (runtimeStateServices == null || runtimeStateServices.getIdempotencyKeyStore() == null
                || request == null || request.getOptions() == null) {
            return true;
        }
        Object configured = request.getOptions().get("idempotencyKey");
        if (!(configured instanceof String) || isBlank((String) configured)) {
            return true;
        }
        String idempotencyKey = ((String) configured).trim();
        AskDataIdempotencyKeyStore store = runtimeStateServices.getIdempotencyKeyStore();
        if (store.recordIfAbsent(request.getProjectContext(), idempotencyKey,
                new AskDataIdempotencyKeyStore.IdempotencyRecord("RUNNING", result.getConversationId()))) {
            return true;
        }
        AskDataIdempotencyKeyStore.IdempotencyRecord existing = store.load(request.getProjectContext(),
                idempotencyKey);
        return existing != null && result.getConversationId().equals(existing.getReferenceId());
    }

    private boolean acquireLease(AskDataRequest request, AskDataResult result, String ownerId) {
        if (runtimeStateServices == null || runtimeStateServices.getLeaseStore() == null) {
            return true;
        }
        return runtimeStateServices.getLeaseStore().tryAcquire(request.getProjectContext(),
                result.getConversationId(), ownerId, Math.max(30000L, multiQuestionTimeoutMillis() * 2L));
    }

    private void releaseLease(AskDataRequest request, AskDataResult result, String ownerId) {
        if (runtimeStateServices == null || runtimeStateServices.getLeaseStore() == null
                || request == null || result == null || isBlank(result.getConversationId())) {
            return;
        }
        try {
            runtimeStateServices.getLeaseStore().release(request.getProjectContext(), result.getConversationId(), ownerId);
        } catch (RuntimeException ignored) {
            // 共享存储仍可按过期时间回收，释放诊断不得覆盖业务结果。
        }
    }

    private void saveAgentState(AskDataRequest request, AskDataResult result, String status, String stage) {
        if (runtimeStateServices == null || runtimeStateServices.getAgentStateStore() == null
                || request == null || request.getProjectContext() == null || result == null
                || isBlank(result.getConversationId())) {
            return;
        }
        List<AskDataAgentStateStore.NodeState> nodes = new ArrayList<AskDataAgentStateStore.NodeState>();
        Set<String> nodeIds = new LinkedHashSet<String>();
        for (AskDataStepLog step : result.getSteps()) {
            if (nodeIds.add(step.getStepKey())) {
                nodes.add(new AskDataAgentStateStore.NodeState(step.getStepKey(),
                        step.isSuccess() ? "SUCCEEDED" : "FAILED"));
            }
        }
        String currentStage = isBlank(stage) ? "unknown" : stage;
        if (nodeIds.add(currentStage)) {
            nodes.add(new AskDataAgentStateStore.NodeState(currentStage, status));
        }
        List<AskDataAgentStateStore.SubTaskState> subTasks =
                new ArrayList<AskDataAgentStateStore.SubTaskState>();
        List<String> plan = new ArrayList<String>();
        for (AskDataResult child : result.getChildResults()) {
            if (child == null || isBlank(child.getQuestionId())) {
                continue;
            }
            String childStatus = isBlank(child.getQuestionState())
                    ? child.isSuccess() ? "SUCCEEDED" : "FAILED" : child.getQuestionState();
            subTasks.add(new AskDataAgentStateStore.SubTaskState(child.getQuestionId(), child.getQuestionId(),
                    childStatus));
            plan.add(child.getQuestionId());
        }
        if (plan.isEmpty()) {
            plan.addAll(completedSteps(result));
        }
        List<AskDataAgentStateStore.ToolSummary> tools =
                new ArrayList<AskDataAgentStateStore.ToolSummary>();
        List<AskDataAgentStateStore.ModelRouteAttempt> attempts =
                new ArrayList<AskDataAgentStateStore.ModelRouteAttempt>();
        for (String modelKey : result.getModelAttempts()) {
            attempts.add(new AskDataAgentStateStore.ModelRouteAttempt(modelKey, "ATTEMPTED", null));
        }
        List<AskDataAgentStateStore.StepLog> steps = new ArrayList<AskDataAgentStateStore.StepLog>();
        for (AskDataStepLog step : result.getSteps()) {
            steps.add(new AskDataAgentStateStore.StepLog(step.getStepKey(),
                    step.isSuccess() ? "SUCCEEDED" : "FAILED", step.getMessage()));
            if ("execute-sql".equals(step.getStepKey()) || step.getStepKey().startsWith("tool:")) {
                tools.add(new AskDataAgentStateStore.ToolSummary(step.getStepKey(),
                        step.isSuccess() ? "SUCCEEDED" : "FAILED", step.getMessage()));
            }
        }
        AskDataAgentStateStore.AgentState state = new AskDataAgentStateStore.AgentState(status, currentStage, null,
                AskDataAgentStateStore.ContextCompressionStatus.NOT_COMPRESSED, nodes,
                subTasks, plan, tools, attempts, steps);
        runtimeStateServices.getAgentStateStore().save(request.getProjectContext(), result.getConversationId(), state);
    }

    private void saveAuditState(AskDataRequest request, AskDataResult result) {
        if (runtimeStateServices == null || runtimeStateServices.getAuditStateStore() == null
                || request == null || request.getProjectContext() == null || result == null
                || isBlank(result.getConversationId())) {
            return;
        }
        AskDataAuditStateStore store = runtimeStateServices.getAuditStateStore();
        AskDataAuditStateStore.AuditState previous = store.load(request.getProjectContext(), result.getConversationId());
        long sequence = System.currentTimeMillis();
        if (previous != null && sequence <= previous.getSequence()) {
            sequence = previous.getSequence() + 1L;
        }
        store.save(request.getProjectContext(), result.getConversationId(),
                new AskDataAuditStateStore.AuditState(result.isSuccess() ? "SUCCEEDED" : "FAILED",
                        isBlank(result.getErrorCode()) ? "terminal" : result.getErrorCode(),
                        sequence));
    }

    private List<AskDataRetrievedDocument> recallLongTermMemory(AskDataRequest request, AskDataResult result,
            AskDataAuthorizationSummary authorizationSummary) {
        if (runtimeStateServices == null || runtimeStateServices.getLongTermMemory() == null
                || request == null || request.getProjectContext() == null || authorizationSummary == null) {
            return Collections.emptyList();
        }
        publishStepEvent(request, result, AskDataEvent.Type.LONG_TERM_MEMORY_RECALL_STARTED,
                "long-term-memory", null, "AskData 长期记忆召回已开始");
        try {
            Set<String> scope = new LinkedHashSet<String>();
            for (AskDataAuthorizedObject object : authorizationSummary.getAuthorizedObjects()) {
                if (object != null && request.getDatasourceKey().equals(object.getDatasourceKey())) {
                    scope.add(object.getTableName() + "." + object.getColumnName());
                }
            }
            List<AskDataMemoryEntry> entries = runtimeStateServices.getLongTermMemory().find(
                    request.getProjectContext(), request.getDatasourceKey(),
                    authorizationSummary.getAuthorizationVersion(), scope, System.currentTimeMillis());
            List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
            for (AskDataMemoryEntry entry : entries) {
                Map<String, Object> metadata = new LinkedHashMap<String, Object>();
                metadata.put("type", "long-term-memory");
                metadata.put("memoryId", entry.getId());
                metadata.put("source", entry.getSource());
                documents.add(new AskDataRetrievedDocument(entry.getContent(), metadata));
            }
            publishStepEvent(request, result, AskDataEvent.Type.LONG_TERM_MEMORY_RECALL_COMPLETED,
                    "long-term-memory", null, "AskData 长期记忆召回已完成");
            return documents;
        } catch (RuntimeException ex) {
            publishStepEvent(request, result, AskDataEvent.Type.LONG_TERM_MEMORY_RECALL_FAILED,
                    "long-term-memory", null, "AskData 长期记忆召回失败");
            return Collections.emptyList();
        }
    }

    private AskDataResult observationResult(AskDataRequest request) {
        AskDataResult result = new AskDataResult();
        if (request != null) {
            result.setConversationId(request.getConversationId());
        }
        return result;
    }

    private String eventStatus(AskDataEvent.Type type) {
        if (type == null) {
            return "RUNNING";
        }
        String name = type.name();
        if (name.endsWith("FAILED")) {
            return "FAILED";
        }
        if (name.endsWith("COMPLETED") || name.endsWith("SAVED") || name.endsWith("EXECUTED")) {
            return "SUCCEEDED";
        }
        if (name.endsWith("CANCELLED")) {
            return "CANCELLED";
        }
        return "RUNNING";
    }

    private List<AskDataRetrievedDocument> retrieveDocuments(AskDataRequest request,
            AskDataAuthorizationSummary authorizationSummary, AskDataResult result) {
        if (vectorRetriever == null) {
            return Collections.emptyList();
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        String[] retrieveTypes = { "schema", "term", "example" };
        Map<String, Object> filters = vectorAuthorizationFilters(request, authorizationSummary);
        for (String retrieveType : retrieveTypes) {
            AskDataVectorQuery query = new AskDataVectorQuery(
                    request == null ? null : request.getProjectContext(), embeddingModelKey(request), retrieveType,
                    request == null ? null : request.getDatasourceKey(), request == null ? null : request.getDatasourceKey(),
                    request == null ? null : request.getQuestion(), topK(), filters);
            try {
                AskDataVectorRetrievalResult retrievalResult = vectorRetriever.retrieveWithResult(query);
                if (retrievalResult != null) {
                    recordEmbeddingAttempts(request, result, retrievalResult.getModelAttempts(),
                            retrievalResult.getActualEmbeddingModelKey());
                    documents.addAll(retrievalResult.getDocuments());
                }
            } catch (AskDataVectorRetrievalException ex) {
                recordEmbeddingAttempts(request, result, ex.getModelAttempts(), null);
                throw ex;
            }
        }
        return documents;
    }

    private void recordEmbeddingAttempts(AskDataRequest request, AskDataResult result,
            List<AskDataModelAttemptTrace> attempts, String actualEmbeddingModelKey) {
        if (result == null) {
            return;
        }
        for (AskDataModelAttemptTrace attempt : attempts == null
                ? Collections.<AskDataModelAttemptTrace>emptyList() : attempts) {
            if (attempt == null) {
                continue;
            }
            if (attempt.getState() == AskDataEvent.ModelState.ATTEMPTED) {
                result.addEmbeddingModelAttempt(attempt.getModelKey());
            }
            if (eventPublisher != null && request != null && request.getProjectContext() != null) {
                safePublish(AskDataEvent.modelAttempt(request.getProjectContext(), result.getConversationId(),
                        AskDataEvent.ModelType.EMBEDDING, attempt.getModelKey(), attempt.getRoutePolicy(),
                        attempt.getState(), attempt.getFailureCode(), attempt.getSwitchReason(),
                        attempt.getActualModelKey(), eventCorrelation(request, result, "embedding", null)));
            }
        }
        if (!isBlank(actualEmbeddingModelKey)) {
            result.setActualEmbeddingModelKey(actualEmbeddingModelKey);
        }
    }

    private Map<String, Object> vectorAuthorizationFilters(AskDataRequest request,
            AskDataAuthorizationSummary authorizationSummary) {
        Map<String, Object> filters = new LinkedHashMap<String, Object>();
        AskDataProjectContext projectContext = request == null ? null : request.getProjectContext();
        if (projectContext != null) {
            filters.put("projectId", projectContext.getProjectId());
            filters.put("operatorId", projectContext.getOperatorId());
        }
        if (request != null && !isBlank(request.getDatasourceKey())) {
            filters.put("datasourceKey", request.getDatasourceKey());
        }
        if (authorizationSummary != null && !isBlank(authorizationSummary.getAuthorizationVersion())) {
            filters.put("authorizationVersion", authorizationSummary.getAuthorizationVersion());
        }
        return filters;
    }

    private String embeddingModelKey(AskDataRequest request) {
        Object value = request == null || request.getOptions() == null
                ? null : request.getOptions().get(EMBEDDING_MODEL_OPTION);
        return value instanceof String && !isBlank((String) value) ? ((String) value).trim() : null;
    }

    private int maxRows(AskDataRequest request) {
        if (request != null && request.getMaxRows() != null && request.getMaxRows().intValue() > 0) {
            return request.getMaxRows().intValue();
        }
        Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
        return defaultMaxRows == null || defaultMaxRows.intValue() <= 0 ? 100 : defaultMaxRows.intValue();
    }

    private int topK() {
        Integer defaultTopK = properties.getVector().getDefaultTopK();
        return defaultTopK == null || defaultTopK.intValue() <= 0 ? 5 : defaultTopK.intValue();
    }

    private String dialect(AskDataRequest request, AskDataMetadata metadata) {
        if (request != null && !isBlank(request.getDialect())) {
            return request.getDialect();
        }
        return metadata.getDialect();
    }

    private boolean isExecutionEnabled(AskDataRequest request) {
        return properties.getSql().isExecuteEnabled() && (request == null || !Boolean.FALSE.equals(request.getExecute()));
    }

    private boolean requestIdentityMatches(AskDataRequest request, AskDataCheckpoint checkpoint,
            AskDataAuthorizationSummary authorizationSummary) {
        if (request == null || checkpoint == null) {
            return false;
        }
        AskDataProjectContext projectContext = request.getProjectContext();
        if (projectContext == null
                || !equals(projectContext.getProjectId(), checkpoint.getProjectId())
                || !equals(projectContext.getOperatorId(), checkpoint.getOperatorId())
                || authorizationSummary == null
                || !equals(authorizationSummary.getAuthorizationVersion(), checkpoint.getAuthorizationVersion())
                || !equals(request.getDatasourceKey(), checkpoint.getDatasourceKey())
                || !equals(request.getModelKey(), checkpoint.getModelKey())
                || !equals(request.getQuestion(), checkpoint.getRequestSummary())
                || !equals(Integer.valueOf(maxRows(request)), checkpoint.getMaxRows())
                || !equals(Boolean.valueOf(isExecutionEnabled(request)), checkpoint.getExecute())
                || !optionsIdentityMatches(request, checkpoint)) {
            return false;
        }
        if (!isBlank(request.getDialect()) && !equals(request.getDialect(), checkpoint.getDialect())) {
            return false;
        }
        return isBlank(request.getConversationId()) || equals(request.getConversationId(), checkpoint.getConversationId());
    }

    private boolean optionsIdentityMatches(AskDataRequest request, AskDataCheckpoint checkpoint) {
        String currentSnapshot = checkpointScopeSnapshot(request);
        String checkpointSnapshot = checkpoint.getRequestOptionsSnapshot();
        return currentSnapshot != null && checkpointSnapshot != null && equals(currentSnapshot, checkpointSnapshot);
    }

    /**
     * 将项目、操作人与调用选项作为检查点恢复身份的一部分持久化。
     *
     * <p>恢复时重新计算该快照，因此旧版本中不含作用域的检查点会被安全拒绝。</p>
     */
    private String checkpointScopeSnapshot(AskDataRequest request) {
        String optionsSnapshot = requestOptionsSnapshot(request);
        AskDataProjectContext projectContext = request == null ? null : request.getProjectContext();
        if (optionsSnapshot == null || projectContext == null) {
            return null;
        }
        return "[" + jsonString(projectContext.getProjectId()) + "," + jsonString(projectContext.getOperatorId())
                + "," + optionsSnapshot + "]";
    }

    private String requestOptionsSnapshot(AskDataRequest request) {
        try {
            return normalizeOptionValue(request == null ? null : request.getOptions(),
                    new IdentityHashMap<Object, Boolean>());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalizeOptionValue(Object value, IdentityHashMap<Object, Boolean> ancestors) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Character) {
            return jsonString(String.valueOf(value));
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (isSafeOptionNumber(value)) {
            return value.toString();
        }
        if (ancestors.containsKey(value)) {
            throw new IllegalArgumentException("Cyclic AskData request options are not supported");
        }
        if (value instanceof Map) {
            ancestors.put(value, Boolean.TRUE);
            try {
                Map<?, ?> source = (Map<?, ?>) value;
                TreeMap<String, Object> ordered = new TreeMap<String, Object>();
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    if (!(entry.getKey() instanceof String)) {
                        throw new IllegalArgumentException("AskData request option keys must be strings");
                    }
                    ordered.put((String) entry.getKey(), entry.getValue());
                }
                StringBuilder snapshot = new StringBuilder("{");
                for (Map.Entry<String, Object> entry : ordered.entrySet()) {
                    if (snapshot.length() > 1) {
                        snapshot.append(',');
                    }
                    snapshot.append(jsonString(entry.getKey())).append(':')
                            .append(normalizeOptionValue(entry.getValue(), ancestors));
                }
                return snapshot.append('}').toString();
            } finally {
                ancestors.remove(value);
            }
        }
        if (value instanceof List) {
            ancestors.put(value, Boolean.TRUE);
            try {
                StringBuilder snapshot = new StringBuilder("[");
                for (Object entry : (List<?>) value) {
                    if (snapshot.length() > 1) {
                        snapshot.append(',');
                    }
                    snapshot.append(normalizeOptionValue(entry, ancestors));
                }
                return snapshot.append(']').toString();
            } finally {
                ancestors.remove(value);
            }
        }
        if (value.getClass().isArray()) {
            ancestors.put(value, Boolean.TRUE);
            try {
                StringBuilder snapshot = new StringBuilder("[");
                int length = java.lang.reflect.Array.getLength(value);
                for (int index = 0; index < length; index++) {
                    if (index > 0) {
                        snapshot.append(',');
                    }
                    snapshot.append(normalizeOptionValue(java.lang.reflect.Array.get(value, index), ancestors));
                }
                return snapshot.append(']').toString();
            } finally {
                ancestors.remove(value);
            }
        }
        throw new IllegalArgumentException("Unsupported AskData request option value");
    }

    private boolean isSafeOptionNumber(Object value) {
        if (value instanceof Float) {
            return !((Float) value).isNaN() && !((Float) value).isInfinite();
        }
        if (value instanceof Double) {
            return !((Double) value).isNaN() && !((Double) value).isInfinite();
        }
        return value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof java.math.BigInteger || value instanceof java.math.BigDecimal;
    }

    private String jsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to normalize AskData request options", ex);
        }
    }

    private String checkpointVersion() {
        return properties.getConversation() == null ? "1" : properties.getConversation().getCheckpointVersion();
    }

    private List<String> textValues(JsonNode node) {
        List<String> values = new ArrayList<String>();
        if (node != null && node.isArray()) {
            for (JsonNode value : node) {
                if (value.isTextual()) {
                    values.add(value.asText());
                }
            }
        }
        return values;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private String message(RuntimeException ex) {
        return isBlank(ex.getMessage()) ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private boolean equals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
