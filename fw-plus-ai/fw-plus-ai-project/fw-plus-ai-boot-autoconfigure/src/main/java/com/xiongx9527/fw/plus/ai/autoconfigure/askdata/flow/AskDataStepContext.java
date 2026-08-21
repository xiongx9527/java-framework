package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AskDataStepContext {

    private final AskDataRequest request;
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    private AskDataMetadata metadata;
    private List<AskDataRetrievedDocument> retrievedDocuments = new ArrayList<AskDataRetrievedDocument>();
    private String prompt;
    private String rawModelResponse;
    private String conversationId;
    private String checkpointId;
    private String resumeToken;
    private String actualModelKey;
    private String sql;
    private Boolean sqlValidated;
    private Boolean sqlExecuted;
    private String executionState;
    private Integer rowCount;
    private String currentStep;
    private List<String> completedSteps = new ArrayList<String>();
    private List<AskDataStepLog> stepLogs = new ArrayList<AskDataStepLog>();
    private String errorCode;

    public AskDataStepContext(AskDataRequest request) {
        this.request = request;
        this.conversationId = request == null ? null : request.getConversationId();
    }

    public AskDataRequest getRequest() {
        return request;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public AskDataMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AskDataMetadata metadata) {
        this.metadata = metadata;
    }

    public List<AskDataRetrievedDocument> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public void setRetrievedDocuments(List<AskDataRetrievedDocument> retrievedDocuments) {
        this.retrievedDocuments = retrievedDocuments == null ? new ArrayList<AskDataRetrievedDocument>()
                : new ArrayList<AskDataRetrievedDocument>(retrievedDocuments);
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getRawModelResponse() {
        return rawModelResponse;
    }

    public void setRawModelResponse(String rawModelResponse) {
        this.rawModelResponse = rawModelResponse;
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getCheckpointId() { return checkpointId; }
    public void setCheckpointId(String checkpointId) { this.checkpointId = checkpointId; }
    public String getResumeToken() { return resumeToken; }
    public void setResumeToken(String resumeToken) { this.resumeToken = resumeToken; }
    public String getActualModelKey() { return actualModelKey; }
    public void setActualModelKey(String actualModelKey) { this.actualModelKey = actualModelKey; }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

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
    public List<AskDataStepLog> getStepLogs() { return stepLogs; }
    public void setStepLogs(List<AskDataStepLog> stepLogs) {
        this.stepLogs = stepLogs == null ? new ArrayList<AskDataStepLog>() : new ArrayList<AskDataStepLog>(stepLogs);
    }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
