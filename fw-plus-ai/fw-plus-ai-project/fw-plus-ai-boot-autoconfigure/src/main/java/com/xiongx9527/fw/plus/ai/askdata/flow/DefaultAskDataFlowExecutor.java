package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataCompressedContext;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelException;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorRetriever;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class DefaultAskDataFlowExecutor implements AskDataFlowExecutor {

    private static final List<String> CHECKPOINT_STEPS = Arrays.asList("metadata", "prompt", "generate-sql",
            "validate-sql", "execute-sql");
    private static final List<String> AGENT_CHECKPOINT_STEPS = Arrays.asList("metadata", "merge-sub-agents", "prompt",
            "generate-sql", "validate-sql", "execute-sql");
    private static final String EXECUTION_EXECUTING = "EXECUTING";
    private static final String EXECUTION_COMPLETED = "COMPLETED";
    private static final String EXECUTION_SKIPPED = "SKIPPED";

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, null, null, null, Collections.<AskDataAgent>emptyList());
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, null,
                Collections.<AskDataAgent>emptyList());
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher) {
        this(properties, modelRegistry, metadataProvider, vectorRetriever, promptTemplateProvider, dialectRegistry,
                sqlValidator, sqlExecutor, conversationStore, contextCompressor, agentDispatcher,
                Collections.<AskDataAgent>emptyList());
    }

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor, AskDataConversationStore conversationStore,
            AskDataContextCompressor contextCompressor, AskDataAgentDispatcher agentDispatcher,
            List<AskDataAgent> mainAgents) {
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
    }

    @Override
    public AskDataResult execute(AskDataRequest request) {
        AskDataResult result = new AskDataResult();
        if (request != null) {
            result.setConversationId(request.getConversationId());
        }
        if (!isResumeRequested(request) && conversationStore != null && contextCompressor != null
                && isBlank(result.getConversationId())) {
            result.setConversationId(UUID.randomUUID().toString());
        }
        AskDataStepContext context = new AskDataStepContext(request);
        if (isResumeRequested(request)) {
            return resume(request, result, context);
        }
        return executeNew(request, result, context);
    }

    private AskDataResult executeNew(AskDataRequest request, AskDataResult result, AskDataStepContext context) {
        AskDataMetadata metadata;
        try {
            metadata = metadataProvider.load(request);
        } catch (RuntimeException ex) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
        }
        if (metadata == null) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, "AskData metadata was not found");
        }
        context.setMetadata(metadata);
        result.addStep("metadata", "Metadata", true, "Metadata loaded");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "metadata", null, null);
        if (checkpointFailure != null) {
            return checkpointFailure;
        }

        AskDataResult agentFailure = runAgentsAndSaveCheckpoint(context, result);
        if (agentFailure != null) {
            return agentFailure;
        }

        return generateSqlAndValidate(request, result, context, metadata);
    }

    private AskDataResult runAgentsAndSaveCheckpoint(AskDataStepContext context, AskDataResult result) {
        if (agentDispatcher == null && mainAgents.isEmpty()) {
            return null;
        }
        for (AskDataAgent agent : mainAgents) {
            String name = agent == null ? "unknown" : agent.getName();
            try {
                AskDataStepResult stepResult = agent == null ? null : agent.execute(context);
                boolean success = stepResult != null && stepResult.isSuccess();
                String message = stepResult == null ? "Main agent returned no result"
                        : success ? stepResult.getOutputSummary() : stepResult.getErrorMessage();
                context.getAttributes().put("askData.mainAgent." + name, message);
                result.addStep("agent:" + name, "Agent " + name, success, message);
            } catch (RuntimeException ex) {
                context.getAttributes().put("askData.mainAgent." + name, message(ex));
                result.addStep("agent:" + name, "Agent " + name, false, message(ex));
            }
        }
        AskDataDispatchResult dispatchResult = null;
        if (agentDispatcher != null) {
            AskDataProperties.Flow flow = properties == null ? null : properties.getFlow();
            Integer timeout = flow == null ? null : flow.getDefaultSubAgentTimeoutMillis();
            try {
                dispatchResult = agentDispatcher.dispatch(new AskDataDispatchRequest(
                        Collections.<AskDataSubAgent>emptyList(), context,
                        timeout == null ? 0L : timeout.longValue(), AskDataDispatchRequest.FailurePolicy.CONTINUE, null));
            } catch (RuntimeException ex) {
                result.addStep("sub-agent:dispatcher", "Sub-agent dispatcher", false, message(ex));
            }
        }
        if (dispatchResult != null) {
            context.getAttributes().put("askData.subAgentResults", dispatchResult.getResults());
            for (AskDataSubAgentResult subAgentResult : dispatchResult.getResults()) {
                String name = subAgentResult.getAgentName() == null ? "unknown" : subAgentResult.getAgentName();
                String message = subAgentResult.isSuccess() ? subAgentResult.getOutputSummary()
                        : subAgentResult.getErrorMessage();
                if (subAgentResult.isTimedOut()) {
                    message = "Timed out: " + message;
                }
                context.getAttributes().put("askData.subAgent." + name, message);
                result.addStep("sub-agent:" + name, "Sub-agent " + name, subAgentResult.isSuccess(), message);
            }
        }
        result.addStep("merge-sub-agents", "Merge agents", true,
                dispatchResult == null ? "Agent outputs merged" : "Merged " + dispatchResult.getResults().size()
                        + " sub-agent result(s)");
        return saveCheckpoint(context, result, "merge-sub-agents", null, null);
    }

    private AskDataResult generateSqlAndValidate(AskDataRequest request, AskDataResult result,
            AskDataStepContext context, AskDataMetadata metadata) {
        int maxRows = maxRows(request);
        List<AskDataRetrievedDocument> documents;
        try {
            documents = retrieveDocuments(request);
        } catch (RuntimeException ex) {
            return failure(result, "prompt", AskDataErrorCode.VECTOR_RETRIEVAL_FAILED, message(ex));
        }
        context.setRetrievedDocuments(documents);
        String prompt;
        try {
            prompt = promptTemplateProvider.buildPrompt(request, metadata, documents, maxRows);
        } catch (RuntimeException ex) {
            return failure(result, "prompt", AskDataErrorCode.MODEL_RESPONSE_INVALID, message(ex));
        }
        context.setPrompt(prompt);
        result.addStep("prompt", "Prompt", true, "Prompt built");

        AskDataChatModel model;
        try {
            model = modelRegistry.getChatModel(request == null ? null : request.getModelKey());
        } catch (AskDataModelException ex) {
            return failure(result, "generate-sql", ex.getErrorCode(), message(ex));
        } catch (RuntimeException ex) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_NOT_FOUND, message(ex));
        }
        if (model == null) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_NOT_FOUND, "AskData chat model was not found");
        }
        AskDataModelResponse modelResponse;
        try {
            modelResponse = model.generate(prompt);
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
        String generatedSql = textValue(modelJson, "sql");
        if (isBlank(generatedSql)) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData model response does not contain SQL");
        }
        context.setSql(generatedSql);
        result.setTables(textValues(modelJson.get("tables")));
        result.setChartType(textValue(modelJson, "chartType"));
        result.setBrief(textValue(modelJson, "brief"));
        result.addStep("generate-sql", "Generate SQL", true, "SQL generated");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "generate-sql", null, null);
        if (checkpointFailure != null) {
            return checkpointFailure;
        }

        return validateAndExecute(request, result, context, metadata, generatedSql);
    }

    private AskDataResult resume(AskDataRequest request, AskDataResult result, AskDataStepContext context) {
        if (conversationStore == null) {
            return failure(result, "resume", AskDataErrorCode.CHECKPOINT_NOT_FOUND, "AskData conversation store is not configured");
        }
        AskDataCheckpoint checkpoint = !isBlank(request.getResumeToken())
                ? conversationStore.loadByResumeToken(request.getResumeToken())
                : conversationStore.loadLatest(request.getConversationId());
        if (checkpoint == null) {
            return failure(result, "resume", AskDataErrorCode.CHECKPOINT_NOT_FOUND, "AskData checkpoint was not found");
        }
        AskDataCheckpoint latest = conversationStore.loadLatest(checkpoint.getConversationId());
        if (latest != null) {
            checkpoint = latest;
        }
        if (!requestIdentityMatches(request, checkpoint)) {
            return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                    "AskData resume request does not match checkpoint identity");
        }
        if (!equals(checkpointVersion(), checkpoint.getVersion()) || !isSafeCheckpoint(checkpoint)
                || EXECUTION_EXECUTING.equals(checkpoint.getExecutionState())) {
            return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                    "AskData checkpoint does not contain a safe resumable state");
        }
        restoreResult(result, checkpoint);
        context.setSql(checkpoint.getSql());
        context.setSqlValidated(checkpoint.getSqlValidated());
        context.setSqlExecuted(checkpoint.getSqlExecuted());
        context.setExecutionState(checkpoint.getExecutionState());
        context.setRowCount(checkpoint.getRowCount());
        context.setCompletedSteps(checkpoint.getCompletedSteps());
        context.setStepLogs(checkpoint.getSteps());
        context.setErrorCode(checkpoint.getErrorCode());
        if (contains(checkpoint, "execute-sql")) {
            result.addStep("execute-sql", "Execute SQL", true, "SQL execution already completed");
            result.setSuccess(true);
            result.addStep("format-result", "Format Result", true, "Result formatted");
            return result;
        }
        if (contains(checkpoint, "validate-sql")) {
            return executeValidatedSql(request, result, context, checkpoint.getSql());
        }
        if (contains(checkpoint, "generate-sql")) {
            AskDataMetadata metadata;
            try {
                metadata = metadataProvider.load(request);
            } catch (RuntimeException ex) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
            }
            if (metadata == null) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND,
                        "AskData metadata was not found");
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
                validation = sqlValidator.validate(checkpoint.getSql(), dialect, maxRows(request), metadata);
            } catch (RuntimeException ex) {
                return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED, message(ex));
            }
            if (validation == null || !validation.isValid()) {
                return validationFailure(result, validation);
            }
            context.setSql(validation.getSql());
            context.setSqlValidated(Boolean.TRUE);
            result.setSql(validation.getSql());
            result.addStep("validate-sql", "Validate SQL", true, "SQL validated");
            AskDataResult checkpointFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
            return checkpointFailure == null ? executeValidatedSql(request, result, context, validation.getSql()) : checkpointFailure;
        }
        if (contains(checkpoint, "metadata") || contains(checkpoint, "merge-sub-agents")) {
            AskDataMetadata metadata;
            try {
                metadata = metadataProvider.load(request);
            } catch (RuntimeException ex) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
            }
            if (metadata == null) {
                return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND,
                        "AskData metadata was not found");
            }
            context.setMetadata(metadata);
            return generateSqlAndValidate(request, result, context, metadata);
        }
        return failure(result, "resume", AskDataErrorCode.RESUME_FAILED,
                "AskData checkpoint cannot restore the metadata required by the next step");
    }

    private AskDataResult validateAndExecute(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            AskDataMetadata metadata, String generatedSql) {
        AskDataDialect dialect;
        try {
            dialect = dialectRegistry.getDialect(dialect(request, metadata));
        } catch (RuntimeException ex) {
            return failure(result, "validate-sql", AskDataErrorCode.DIALECT_RESOLUTION_FAILED, message(ex));
        }
        AskDataSqlValidationResult validation;
        try {
            validation = sqlValidator.validate(generatedSql, dialect, maxRows(request), metadata);
        } catch (RuntimeException ex) {
            return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED, message(ex));
        }
        if (validation == null || !validation.isValid()) {
            return validationFailure(result, validation);
        }
        result.setSql(validation.getSql());
        context.setSql(validation.getSql());
        context.setSqlValidated(Boolean.TRUE);
        result.addStep("validate-sql", "Validate SQL", true, "SQL validated");
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
        return checkpointFailure == null ? executeValidatedSql(request, result, context, validation.getSql()) : checkpointFailure;
    }

    private AskDataResult executeValidatedSql(AskDataRequest request, AskDataResult result, AskDataStepContext context,
            String sql) {
        if (isExecutionEnabled(request)) {
            context.setExecutionState(EXECUTION_EXECUTING);
            AskDataResult inFlightFailure = saveCheckpoint(context, result, "validate-sql", Boolean.TRUE, null);
            if (inFlightFailure != null) {
                return inFlightFailure;
            }
            AskDataSqlExecutionResult execution;
            try {
                execution = sqlExecutor.execute(request, sql);
            } catch (RuntimeException ex) {
                return failure(result, "execute-sql", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
            }
            if (execution == null || !execution.isSuccess()) {
                String errorCode = execution == null || isBlank(execution.getErrorCode())
                        ? AskDataErrorCode.SQL_EXECUTION_FAILED : execution.getErrorCode();
                String errorMessage = execution == null ? "AskData SQL execution failed" : execution.getErrorMessage();
                return failure(result, "execute-sql", errorCode, errorMessage);
            }
            result.setColumns(execution.getColumns());
            result.setRows(execution.getRows());
            result.setRowCount(Integer.valueOf(execution.getRowCount()));
            context.setRowCount(Integer.valueOf(execution.getRowCount()));
            context.setSqlExecuted(Boolean.TRUE);
            context.setExecutionState(EXECUTION_COMPLETED);
            result.addStep("execute-sql", "Execute SQL", true, "SQL executed");
        } else {
            context.setSqlExecuted(Boolean.FALSE);
            context.setExecutionState(EXECUTION_SKIPPED);
            result.addStep("execute-sql", "Execute SQL", true, "SQL execution skipped");
        }
        AskDataResult checkpointFailure = saveCheckpoint(context, result, "execute-sql", context.getSqlValidated(),
                context.getSqlExecuted());
        if (checkpointFailure != null) {
            return checkpointFailure;
        }
        result.setSuccess(true);
        result.addStep("format-result", "Format Result", true, "Result formatted");
        return result;
    }

    private AskDataResult validationFailure(AskDataResult result, AskDataSqlValidationResult validation) {
        String errorCode = validation == null || isBlank(validation.getErrorCode()) ? AskDataErrorCode.SQL_REJECTED
                : validation.getErrorCode();
        String errorMessage = validation == null ? "AskData SQL validation failed" : validation.getMessage();
        return failure(result, "validate-sql", errorCode, errorMessage);
    }

    private AskDataResult saveCheckpoint(AskDataStepContext context, AskDataResult result, String currentStep,
            Boolean sqlValidated, Boolean sqlExecuted) {
        if (conversationStore == null || contextCompressor == null || isBlank(result.getConversationId())) {
            return null;
        }
        try {
            context.setCompletedSteps(completedSteps(result));
            context.setStepLogs(result.getSteps());
            context.setSqlValidated(sqlValidated == null ? context.getSqlValidated() : sqlValidated);
            context.setSqlExecuted(sqlExecuted == null ? context.getSqlExecuted() : sqlExecuted);
            context.setRowCount(result.getRowCount());
            AskDataCompressedContext compressed = contextCompressor.compress(context);
            AskDataCheckpoint checkpoint = new AskDataCheckpoint();
            AskDataRequest request = context.getRequest();
            checkpoint.setConversationId(result.getConversationId());
            checkpoint.setCheckpointId(UUID.randomUUID().toString());
            checkpoint.setResumeToken(UUID.randomUUID().toString());
            checkpoint.setRequestSummary(request == null ? null : request.getQuestion());
            checkpoint.setModelKey(request == null ? null : request.getModelKey());
            checkpoint.setDatasourceKey(request == null ? null : request.getDatasourceKey());
            checkpoint.setMaxRows(Integer.valueOf(maxRows(request)));
            checkpoint.setExecute(Boolean.valueOf(isExecutionEnabled(request)));
            checkpoint.setRequestOptionsSnapshot(requestOptionsSnapshot(request));
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
            AskDataCheckpoint saved = conversationStore.save(checkpoint);
            AskDataCheckpoint responseCheckpoint = saved == null ? checkpoint : saved;
            result.setCheckpointId(responseCheckpoint.getCheckpointId());
            result.setResumeToken(responseCheckpoint.getResumeToken());
            return null;
        } catch (RuntimeException ex) {
            return failure(result, currentStep, AskDataErrorCode.CONTEXT_COMPRESSION_FAILED, message(ex));
        }
    }

    private void restoreResult(AskDataResult result, AskDataCheckpoint checkpoint) {
        result.setConversationId(checkpoint.getConversationId());
        result.setCheckpointId(checkpoint.getCheckpointId());
        result.setResumeToken(checkpoint.getResumeToken());
        result.setSql(checkpoint.getSql());
        result.setRowCount(checkpoint.getRowCount());
        result.setSteps(new ArrayList<AskDataStepLog>(checkpoint.getSteps()));
        for (String completedStep : checkpoint.getCompletedSteps()) {
            if (!hasSuccessfulStep(result.getSteps(), completedStep)) {
                result.addStep(completedStep, completedStep, true, "Checkpoint restored");
            }
        }
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

    private List<AskDataRetrievedDocument> retrieveDocuments(AskDataRequest request) {
        if (vectorRetriever == null) {
            return Collections.emptyList();
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        String[] retrieveTypes = { "schema", "term", "example" };
        for (String retrieveType : retrieveTypes) {
            List<AskDataRetrievedDocument> retrieved = vectorRetriever.retrieve(new AskDataVectorQuery(retrieveType,
                    request == null ? null : request.getDatasourceKey(), request == null ? null : request.getDatasourceKey(),
                    request == null ? null : request.getQuestion(), topK(), Collections.<String, Object>emptyMap()));
            if (retrieved != null) {
                documents.addAll(retrieved);
            }
        }
        return documents;
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

    private boolean requestIdentityMatches(AskDataRequest request, AskDataCheckpoint checkpoint) {
        if (request == null || checkpoint == null) {
            return false;
        }
        if (!equals(request.getDatasourceKey(), checkpoint.getDatasourceKey())
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
        String currentSnapshot = requestOptionsSnapshot(request);
        String checkpointSnapshot = checkpoint.getRequestOptionsSnapshot();
        return currentSnapshot != null && checkpointSnapshot != null && equals(currentSnapshot, checkpointSnapshot);
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
