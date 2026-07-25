package com.xiongx9527.fw.plus.ai.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataStepContext;
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
        AskDataStepContext context = new AskDataStepContext(request);
        context.setPrompt(repeat("prompt ", 80));
        context.setRawModelResponse(repeat("model ", 80));
        context.setSql("select * from orders");
        context.setSqlValidated(Boolean.TRUE);
        context.setCompletedSteps(Arrays.asList("metadata", "generate-sql", "validate-sql"));
        context.setErrorCode("SQL_REJECTED");
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
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}
