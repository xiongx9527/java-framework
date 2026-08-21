package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import java.util.ArrayList;
import java.util.List;

public class AskDataCompressedContext {

    private String compressedSummary;
    private String projectId;
    private String operatorId;
    private String conversationId;
    private String checkpointId;
    private String resumeToken;
    private String modelKey;
    private String actualModelKey;
    private String datasourceKey;
    private String dialect;
    private String sql;
    private Boolean sqlValidated;
    private Boolean sqlExecuted;
    private String executionState;
    private Integer rowCount;
    private String currentStep;
    private List<String> completedSteps = new ArrayList<String>();
    private String errorCode;

    public String getCompressedSummary() { return compressedSummary; }
    public void setCompressedSummary(String compressedSummary) { this.compressedSummary = compressedSummary; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getResumeToken() { return resumeToken; }
    public void setResumeToken(String resumeToken) { this.resumeToken = resumeToken; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getActualModelKey() { return actualModelKey; }
    public void setActualModelKey(String actualModelKey) { this.actualModelKey = actualModelKey; }
    public String getDatasourceKey() { return datasourceKey; }
    public void setDatasourceKey(String datasourceKey) { this.datasourceKey = datasourceKey; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public Boolean getSqlValidated() { return sqlValidated; }
    public void setSqlValidated(Boolean sqlValidated) { this.sqlValidated = sqlValidated; }
    public Boolean getSqlExecuted() { return sqlExecuted; }
    public void setSqlExecuted(Boolean sqlExecuted) { this.sqlExecuted = sqlExecuted; }
    public String getExecutionState() { return executionState; }
    public void setExecutionState(String executionState) { this.executionState = executionState; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public List<String> getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(List<String> completedSteps) {
        this.completedSteps = completedSteps == null ? new ArrayList<String>() : new ArrayList<String>(completedSteps);
    }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
