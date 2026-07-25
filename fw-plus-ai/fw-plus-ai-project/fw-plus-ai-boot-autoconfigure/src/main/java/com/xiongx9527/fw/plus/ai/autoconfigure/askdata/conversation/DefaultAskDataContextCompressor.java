package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;

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
            compressed.setConversationId(request.getConversationId());
            compressed.setModelKey(request.getModelKey());
            compressed.setDatasourceKey(request.getDatasourceKey());
            compressed.setDialect(request.getDialect());
        }
        compressed.setSql(context.getSql());
        compressed.setSqlValidated(context.getSqlValidated());
        compressed.setCompletedSteps(context.getCompletedSteps());
        compressed.setErrorCode(context.getErrorCode());
        compressed.setCompressedSummary(limit(text(context), threshold()));
        return compressed;
    }

    private String text(AskDataStepContext context) {
        StringBuilder summary = new StringBuilder();
        append(summary, context.getPrompt());
        append(summary, context.getRawModelResponse());
        for (AskDataRetrievedDocument document : context.getRetrievedDocuments()) {
            append(summary, document == null ? null : document.getContent());
        }
        for (AskDataStepLog step : context.getStepLogs()) {
            append(summary, step == null ? null : step.getMessage());
        }
        return summary.toString();
    }

    private void append(StringBuilder target, String value) {
        if (value != null && value.length() > 0) {
            if (target.length() > 0) {
                target.append('\n');
            }
            target.append(value);
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
