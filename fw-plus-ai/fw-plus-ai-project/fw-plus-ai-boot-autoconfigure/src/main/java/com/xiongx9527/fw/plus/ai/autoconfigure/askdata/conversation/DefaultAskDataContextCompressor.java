package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

public class DefaultAskDataContextCompressor implements AskDataContextCompressor {

    private static final int DEFAULT_THRESHOLD = 12000;

    private final AskDataProperties properties;

    public DefaultAskDataContextCompressor(AskDataProperties properties) {
        this.properties = properties;
    }

    @Override
    public AskDataCompressedContext compress(AskDataStepContext context) {
        if (context == null) {
            throw new IllegalArgumentException("AskData step context must not be null");
        }
        AskDataCompressedContext compressed = new AskDataCompressedContext();
        AskDataRequest request = context.getRequest();
        if (request != null) {
            AskDataProjectContext projectContext = request.getProjectContext();
            if (projectContext != null) {
                compressed.setProjectId(projectContext.getProjectId());
                compressed.setOperatorId(projectContext.getOperatorId());
            }
            compressed.setModelKey(request.getModelKey());
            compressed.setDatasourceKey(request.getDatasourceKey());
            compressed.setDialect(request.getDialect());
        }
        compressed.setConversationId(context.getConversationId());
        compressed.setCheckpointId(context.getCheckpointId());
        compressed.setResumeToken(context.getResumeToken());
        compressed.setActualModelKey(context.getActualModelKey());
        compressed.setSql(context.getSql());
        compressed.setSqlValidated(context.getSqlValidated());
        compressed.setSqlExecuted(context.getSqlExecuted());
        compressed.setExecutionState(context.getExecutionState());
        compressed.setRowCount(context.getRowCount());
        compressed.setCurrentStep(context.getCurrentStep());
        compressed.setCompletedSteps(context.getCompletedSteps());
        compressed.setErrorCode(context.getErrorCode());
        compressed.setCompressedSummary(limit(summary(context), threshold()));
        return compressed;
    }

    private String summary(AskDataStepContext context) {
        StringBuilder summary = new StringBuilder();
        append(summary, "completedSteps", context.getCompletedSteps());
        append(summary, "currentStep", context.getCurrentStep());
        append(summary, "checkpointPresent", Boolean.valueOf(context.getCheckpointId() != null));
        append(summary, "resumeTokenPresent", Boolean.valueOf(context.getResumeToken() != null));
        append(summary, "sqlPresent", Boolean.valueOf(context.getSql() != null && context.getSql().length() > 0));
        append(summary, "sqlValidated", context.getSqlValidated());
        append(summary, "sqlExecuted", context.getSqlExecuted());
        append(summary, "executionState", context.getExecutionState());
        append(summary, "rowCount", context.getRowCount());
        append(summary, "retrievedDocumentCount", Integer.valueOf(context.getRetrievedDocuments().size()));
        append(summary, "stepLogCount", Integer.valueOf(context.getStepLogs().size()));
        append(summary, "errorCode", context.getErrorCode());
        return summary.toString();
    }

    private void append(StringBuilder target, String label, Object value) {
        if (value != null) {
            if (target.length() > 0) { target.append(';'); }
            target.append(label).append('=').append(value);
        }
    }

    private String limit(String value, int threshold) {
        return value.length() <= threshold ? value : value.substring(0, threshold);
    }

    private int threshold() {
        Integer configured = properties == null || properties.getConversation() == null ? null
                : properties.getConversation().getCompressionThresholdChars();
        return configured == null || configured.intValue() <= 0 ? DEFAULT_THRESHOLD : configured.intValue();
    }
}
