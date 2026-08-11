package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AskDataResult {

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String sql;
    private List<String> tables = new ArrayList<String>();
    private String chartType;
    private String brief;
    private List<AskDataColumn> columns = new ArrayList<AskDataColumn>();
    private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private Integer rowCount;
    private List<AskDataStepLog> steps = new ArrayList<AskDataStepLog>();
    private String conversationId;
    private String resumeToken;
    private String checkpointId;
    private String rawModelResponse;
    private String actualModelKey;
    private List<String> modelAttempts = new ArrayList<String>();
    private String actualEmbeddingModelKey;
    private List<String> embeddingModelAttempts = new ArrayList<String>();
    /**
     * 显式多问题请求的子执行结果。
     *
     * <p>单问题请求保持为空列表，避免改变既有结果字段及其成功语义。每个子结果都保留自身的步骤审计，
     * 聚合结果不会混入子问题的 SQL、模型响应或其他执行细节。</p>
     */
    private List<AskDataResult> childResults = new ArrayList<AskDataResult>();

    public static AskDataResult failure(String errorCode, String errorMessage) {
        AskDataResult result = new AskDataResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public void addStep(String stepKey, String stepName, boolean stepSuccess, String message) {
        steps.add(new AskDataStepLog(stepKey, stepName, stepSuccess, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief;
    }

    public List<AskDataColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<AskDataColumn> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public List<AskDataStepLog> getSteps() {
        return steps;
    }

    public void setSteps(List<AskDataStepLog> steps) {
        this.steps = steps;
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

    public String getCheckpointId() {
        return checkpointId;
    }

    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }

    public String getRawModelResponse() {
        return rawModelResponse;
    }

    public void setRawModelResponse(String rawModelResponse) {
        this.rawModelResponse = rawModelResponse;
    }

    public String getActualModelKey() {
        return actualModelKey;
    }

    public void setActualModelKey(String actualModelKey) {
        this.actualModelKey = actualModelKey;
    }

    public List<String> getModelAttempts() {
        return new ArrayList<String>(modelAttempts);
    }

    public void setModelAttempts(List<String> modelAttempts) {
        this.modelAttempts = modelAttempts == null ? new ArrayList<String>() : new ArrayList<String>(modelAttempts);
    }

    public void addModelAttempt(String modelKey) {
        if (modelKey != null && modelKey.trim().length() > 0) {
            modelAttempts.add(modelKey.trim());
        }
    }

    public String getActualEmbeddingModelKey() {
        return actualEmbeddingModelKey;
    }

    public void setActualEmbeddingModelKey(String actualEmbeddingModelKey) {
        this.actualEmbeddingModelKey = actualEmbeddingModelKey;
    }

    public List<String> getEmbeddingModelAttempts() {
        return new ArrayList<String>(embeddingModelAttempts);
    }

    public void setEmbeddingModelAttempts(List<String> embeddingModelAttempts) {
        this.embeddingModelAttempts = embeddingModelAttempts == null
                ? new ArrayList<String>() : new ArrayList<String>(embeddingModelAttempts);
    }

    public void addEmbeddingModelAttempt(String modelKey) {
        if (modelKey != null && modelKey.trim().length() > 0) {
            embeddingModelAttempts.add(modelKey.trim());
        }
    }

    public List<AskDataResult> getChildResults() {
        return childResults;
    }

    public void setChildResults(List<AskDataResult> childResults) {
        this.childResults = childResults == null ? new ArrayList<AskDataResult>() : childResults;
    }
}
