package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import java.util.ArrayList;
import java.util.List;

public class AskDataCompressedContext {

    private String compressedSummary;
    private String conversationId;
    private String modelKey;
    private String datasourceKey;
    private String dialect;
    private String sql;
    private Boolean sqlValidated;
    private List<String> completedSteps = new ArrayList<String>();
    private String errorCode;

    public String getCompressedSummary() { return compressedSummary; }
    public void setCompressedSummary(String compressedSummary) { this.compressedSummary = compressedSummary; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getDatasourceKey() { return datasourceKey; }
    public void setDatasourceKey(String datasourceKey) { this.datasourceKey = datasourceKey; }
    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public Boolean getSqlValidated() { return sqlValidated; }
    public void setSqlValidated(Boolean sqlValidated) { this.sqlValidated = sqlValidated; }
    public List<String> getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(List<String> completedSteps) {
        this.completedSteps = completedSteps == null ? new ArrayList<String>() : new ArrayList<String>(completedSteps);
    }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
