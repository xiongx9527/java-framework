package com.xiongx9527.fw.plus.ai.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AskDataCheckpoint {

    private String conversationId;
    private String checkpointId;
    private String resumeToken;
    private String requestSummary;
    private String modelKey;
    private String datasourceKey;
    private String dialect;
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
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getResumeToken() { return resumeToken; }
    public void setResumeToken(String resumeToken) { this.resumeToken = resumeToken; }
    public String getRequestSummary() { return requestSummary; }
    public void setRequestSummary(String requestSummary) { this.requestSummary = requestSummary; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getDatasourceKey() { return datasourceKey; }
    public void setDatasourceKey(String datasourceKey) { this.datasourceKey = datasourceKey; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
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
