package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;

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
    private String sql;
    private Boolean sqlValidated;
    private Boolean sqlExecuted;
    private Integer rowCount;
    private List<String> completedSteps = new ArrayList<String>();
    private List<AskDataStepLog> stepLogs = new ArrayList<AskDataStepLog>();
    private String errorCode;

    public AskDataStepContext(AskDataRequest request) {
        this.request = request;
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
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
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
