package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelAttempt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AskDataCheckpoint {

    private String conversationId;
    private String projectId;
    private String operatorId;
    private String authorizationVersion;
    private String checkpointId;
    private String resumeToken;
    private String requestSummary;
    private String modelKey;
    private String actualModelKey;
    private List<String> modelAttempts = new ArrayList<String>();
    private String actualEmbeddingModelKey;
    private List<String> embeddingModelAttempts = new ArrayList<String>();
    private List<AskDataModelAttempt> modelAttemptDetails = new ArrayList<AskDataModelAttempt>();
    private List<AskDataModelAttempt> embeddingModelAttemptDetails = new ArrayList<AskDataModelAttempt>();
    private String modelRouteSnapshot;
    private Boolean multiQuestion;
    private String multiQuestionPlanSnapshot;
    private List<AskDataResult> childResults = new ArrayList<AskDataResult>();
    private String datasourceKey;
    private String dialect;
    private Integer maxRows;
    private Boolean execute;
    private String requestOptionsSnapshot;
    private String executionState;
    private List<String> completedSteps = new ArrayList<String>();
    private String currentStep;
    private String sql;
    private Boolean sqlValidated;
    private Boolean sqlExecuted;
    private Integer rowCount;
    private String compressedSummary;
    private List<AskDataStepLog> steps = new ArrayList<AskDataStepLog>();
    private String errorCode;
    private String version;
    private Date createdAt;

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getAuthorizationVersion() { return authorizationVersion; }
    public void setAuthorizationVersion(String authorizationVersion) {
        this.authorizationVersion = authorizationVersion;
    }
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getResumeToken() { return resumeToken; }
    public void setResumeToken(String resumeToken) { this.resumeToken = resumeToken; }
    public String getRequestSummary() { return requestSummary; }
    public void setRequestSummary(String requestSummary) { this.requestSummary = requestSummary; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getActualModelKey() { return actualModelKey; }
    public void setActualModelKey(String actualModelKey) { this.actualModelKey = actualModelKey; }
    public List<String> getModelAttempts() { return new ArrayList<String>(modelAttempts); }
    public void setModelAttempts(List<String> modelAttempts) {
        this.modelAttempts = modelAttempts == null ? new ArrayList<String>() : new ArrayList<String>(modelAttempts);
    }
    public String getActualEmbeddingModelKey() { return actualEmbeddingModelKey; }
    public void setActualEmbeddingModelKey(String actualEmbeddingModelKey) {
        this.actualEmbeddingModelKey = actualEmbeddingModelKey;
    }
    public List<String> getEmbeddingModelAttempts() { return new ArrayList<String>(embeddingModelAttempts); }
    public void setEmbeddingModelAttempts(List<String> embeddingModelAttempts) {
        this.embeddingModelAttempts = embeddingModelAttempts == null
                ? new ArrayList<String>() : new ArrayList<String>(embeddingModelAttempts);
    }
    public List<AskDataModelAttempt> getModelAttemptDetails() {
        return new ArrayList<AskDataModelAttempt>(modelAttemptDetails);
    }
    public void setModelAttemptDetails(List<AskDataModelAttempt> modelAttemptDetails) {
        this.modelAttemptDetails = modelAttemptDetails == null ? new ArrayList<AskDataModelAttempt>()
                : new ArrayList<AskDataModelAttempt>(modelAttemptDetails);
    }
    public List<AskDataModelAttempt> getEmbeddingModelAttemptDetails() {
        return new ArrayList<AskDataModelAttempt>(embeddingModelAttemptDetails);
    }
    public void setEmbeddingModelAttemptDetails(List<AskDataModelAttempt> embeddingModelAttemptDetails) {
        this.embeddingModelAttemptDetails = embeddingModelAttemptDetails == null
                ? new ArrayList<AskDataModelAttempt>()
                : new ArrayList<AskDataModelAttempt>(embeddingModelAttemptDetails);
    }
    public String getModelRouteSnapshot() { return modelRouteSnapshot; }
    public void setModelRouteSnapshot(String modelRouteSnapshot) { this.modelRouteSnapshot = modelRouteSnapshot; }
    public Boolean getMultiQuestion() { return multiQuestion; }
    public void setMultiQuestion(Boolean multiQuestion) { this.multiQuestion = multiQuestion; }
    public String getMultiQuestionPlanSnapshot() { return multiQuestionPlanSnapshot; }
    public void setMultiQuestionPlanSnapshot(String multiQuestionPlanSnapshot) {
        this.multiQuestionPlanSnapshot = multiQuestionPlanSnapshot;
    }
    public List<AskDataResult> getChildResults() { return new ArrayList<AskDataResult>(childResults); }
    public void setChildResults(List<AskDataResult> childResults) {
        this.childResults = childResults == null ? new ArrayList<AskDataResult>()
                : new ArrayList<AskDataResult>(childResults);
    }
    public String getDatasourceKey() { return datasourceKey; }
    public void setDatasourceKey(String datasourceKey) { this.datasourceKey = datasourceKey; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
    public Integer getMaxRows() { return maxRows; }
    public void setMaxRows(Integer maxRows) { this.maxRows = maxRows; }
    public Boolean getExecute() { return execute; }
    public void setExecute(Boolean execute) { this.execute = execute; }
    public String getRequestOptionsSnapshot() { return requestOptionsSnapshot; }
    public void setRequestOptionsSnapshot(String requestOptionsSnapshot) {
        this.requestOptionsSnapshot = requestOptionsSnapshot;
    }
    public String getExecutionState() { return executionState; }
    public void setExecutionState(String executionState) { this.executionState = executionState; }
    public List<String> getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(List<String> completedSteps) {
        this.completedSteps = completedSteps == null ? new ArrayList<String>() : new ArrayList<String>(completedSteps);
    }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public Boolean getSqlValidated() { return sqlValidated; }
    public void setSqlValidated(Boolean sqlValidated) { this.sqlValidated = sqlValidated; }
    public Boolean getSqlExecuted() { return sqlExecuted; }
    public void setSqlExecuted(Boolean sqlExecuted) { this.sqlExecuted = sqlExecuted; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getCompressedSummary() { return compressedSummary; }
    public void setCompressedSummary(String compressedSummary) { this.compressedSummary = compressedSummary; }
    public List<AskDataStepLog> getSteps() { return steps; }
    public void setSteps(List<AskDataStepLog> steps) {
        this.steps = steps == null ? new ArrayList<AskDataStepLog>() : new ArrayList<AskDataStepLog>(steps);
    }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Date getCreatedAt() { return createdAt == null ? null : new Date(createdAt.getTime()); }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt == null ? null : new Date(createdAt.getTime()); }
}
