package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AskDataApiModelTest {

    @Test
    public void shouldKeepRequestLevelModelKeyAndOptions() {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("统计本月订单数");
        request.setDatasourceKey("order");
        request.setDialect("mysql");
        request.setModelKey("gpt4o-mini");
        request.setConversationId("conv-1");
        request.setResumeToken("resume-1");
        request.setExecute(Boolean.TRUE);
        request.setMaxRows(50);
        request.setOptions(Collections.<String, Object>singletonMap("tenantId", "9527"));

        assertEquals("gpt4o-mini", request.getModelKey());
        assertEquals("resume-1", request.getResumeToken());
        assertEquals("9527", request.getOptions().get("tenantId"));
    }

    @Test
    public void shouldCreateFailureResultWithStepLog() {
        AskDataResult result = AskDataResult.failure(AskDataErrorCode.SQL_REJECTED, "危险 SQL 被拒绝");
        result.addStep("validate-sql", "SQL 校验", false, "DROP 被拒绝");

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(1, result.getSteps().size());
        assertEquals("validate-sql", result.getSteps().get(0).getStepKey());
    }
}
