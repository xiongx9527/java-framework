package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultAskDataContextCompressorTest {

    @Test
    public void shouldCompressTextWithoutReplacingStructuredContextFields() {
        AskDataProperties properties = new AskDataProperties();
        properties.getConversation().setCompressionThresholdChars(Integer.valueOf(120));
        AskDataRequest request = new AskDataRequest();
        request.setConversationId("conversation-1");
        request.setModelKey("model-1");
        request.setDatasourceKey("datasource-1");
        request.setDialect("mysql");
        request.setProjectContext(new AskDataProjectContext("project-1", "operator-1"));
        AskDataStepContext context = new AskDataStepContext(request);
        context.setPrompt(repeat("prompt ", 80));
        context.setRawModelResponse(repeat("model ", 80));
        context.setSql("select * from orders");
        context.setSqlValidated(Boolean.TRUE);
        context.setCompletedSteps(Arrays.asList("metadata", "generate-sql", "validate-sql"));
        context.setErrorCode("SQL_REJECTED");
        context.setCurrentStep("validate-sql");
        context.setCheckpointId("checkpoint-1");
        context.setResumeToken("resume-1");
        context.setActualModelKey("provider-model-1");
        context.setExecutionState("COMPLETED");
        context.setSqlExecuted(Boolean.TRUE);
        context.setRowCount(Integer.valueOf(7));
        context.setStepLogs(Arrays.asList(new AskDataStepLog("validate-sql", "Validate SQL", true,
                repeat("detail ", 30))));

        AskDataCompressedContext compressed = new DefaultAskDataContextCompressor(properties).compress(context);

        assertTrue(compressed.getCompressedSummary().length() <= 120);
        assertEquals("conversation-1", compressed.getConversationId());
        assertEquals("model-1", compressed.getModelKey());
        assertEquals("datasource-1", compressed.getDatasourceKey());
        assertEquals("mysql", compressed.getDialect());
        assertEquals("select * from orders", compressed.getSql());
        assertEquals(Boolean.TRUE, compressed.getSqlValidated());
        assertEquals(Arrays.asList("metadata", "generate-sql", "validate-sql"), compressed.getCompletedSteps());
        assertEquals("SQL_REJECTED", compressed.getErrorCode());
        assertEquals("project-1", compressed.getProjectId());
        assertEquals("operator-1", compressed.getOperatorId());
        assertEquals("validate-sql", compressed.getCurrentStep());
        assertEquals("checkpoint-1", compressed.getCheckpointId());
        assertEquals("resume-1", compressed.getResumeToken());
        assertEquals("provider-model-1", compressed.getActualModelKey());
        assertEquals("COMPLETED", compressed.getExecutionState());
        assertEquals(Boolean.TRUE, compressed.getSqlExecuted());
        assertEquals(Integer.valueOf(7), compressed.getRowCount());
        assertTrue(compressed.getCompressedSummary().contains("completedSteps="));
        assertTrue(!compressed.getCompressedSummary().contains("prompt prompt"));
    }

    @Test
    public void shouldFallBackToDeterministicCompressionAndPublishDegradedEvent() {
        AskDataProperties properties = new AskDataProperties();
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单数量");
        request.setConversationId("conversation-1");
        request.setProjectContext(new AskDataProjectContext("project-1", "operator-1"));
        request.setDatasourceKey("orders");
        AskDataStepContext context = new AskDataStepContext(request);
        context.setCurrentStep("validate-sql");
        context.setSql("select count(*) from orders");
        context.setSqlValidated(Boolean.TRUE);
        final java.util.List<AskDataEvent> events = new java.util.ArrayList<AskDataEvent>();
        AskDataContextCompressor semantic = ignored -> { throw new IllegalStateException("timeout"); };
        FallbackAskDataContextCompressor compressor = new FallbackAskDataContextCompressor(semantic,
                new DefaultAskDataContextCompressor(properties), events::add);

        AskDataCompressedContext compressed = compressor.compress(context);

        assertEquals("project-1", compressed.getProjectId());
        assertEquals("operator-1", compressed.getOperatorId());
        assertEquals("validate-sql", compressed.getCurrentStep());
        assertEquals(Boolean.TRUE, compressed.getSqlValidated());
        assertEquals(1, events.size());
        assertEquals(AskDataEvent.Type.CONTEXT_COMPRESSION_DEGRADED, events.get(0).getType());
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}
