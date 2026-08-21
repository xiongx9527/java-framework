package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelSelection;
import com.xiongx9527.fw.plus.ai.askdata.publishing.AskDataPublishRequest;

import java.util.Map;

public class AskDataRequest {

    private String question;
    /**
     * Core AskData 在桥接期间传入的项目与操作人隔离上下文，供旧流程内部的权限和审计逻辑读取。
     * 调用方不得伪造该上下文，真实权限校验和审计执行仍由后续桥接或业务覆盖实现负责。
     */
    private AskDataProjectContext projectContext;
    private String datasourceKey;
    private String dialect;
    private String modelKey;
    private AskDataModelSelection chatModelSelection = AskDataModelSelection.empty();
    private AskDataModelSelection embeddingModelSelection = AskDataModelSelection.empty();
    private AskDataPublishRequest publishRequest;
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

    public AskDataProjectContext getProjectContext() {
        return projectContext;
    }

    public void setProjectContext(AskDataProjectContext projectContext) {
        this.projectContext = projectContext;
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

    public AskDataModelSelection getChatModelSelection() { return chatModelSelection; }
    public void setChatModelSelection(AskDataModelSelection chatModelSelection) {
        this.chatModelSelection = chatModelSelection == null ? AskDataModelSelection.empty() : chatModelSelection;
    }
    public AskDataModelSelection getEmbeddingModelSelection() { return embeddingModelSelection; }
    public void setEmbeddingModelSelection(AskDataModelSelection embeddingModelSelection) {
        this.embeddingModelSelection = embeddingModelSelection == null
                ? AskDataModelSelection.empty() : embeddingModelSelection;
    }
    public AskDataPublishRequest getPublishRequest() { return publishRequest; }
    public void setPublishRequest(AskDataPublishRequest publishRequest) { this.publishRequest = publishRequest; }

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
