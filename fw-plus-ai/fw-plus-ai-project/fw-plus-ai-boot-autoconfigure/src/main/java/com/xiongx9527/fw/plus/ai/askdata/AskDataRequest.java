package com.xiongx9527.fw.plus.ai.askdata;

import java.util.Map;

public class AskDataRequest {

    private String question;
    private String datasourceKey;
    private String dialect;
    private String modelKey;
    private String conversationId;
    private String resumeToken;
    private Boolean execute;
    private Integer maxRows;
    private Map<String, Object> options;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDatasourceKey() {
        return datasourceKey;
    }

    public void setDatasourceKey(String datasourceKey) {
        this.datasourceKey = datasourceKey;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getModelKey() {
        return modelKey;
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    public void setResumeToken(String resumeToken) {
        this.resumeToken = resumeToken;
    }

    public Boolean getExecute() {
        return execute;
    }

    public void setExecute(Boolean execute) {
        this.execute = execute;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(Integer maxRows) {
        this.maxRows = maxRows;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
