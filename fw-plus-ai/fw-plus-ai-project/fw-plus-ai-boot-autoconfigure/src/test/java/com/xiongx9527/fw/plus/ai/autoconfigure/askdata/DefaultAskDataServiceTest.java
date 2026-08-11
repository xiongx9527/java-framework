package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultAskDataServiceTest {

    @Test
    public void shouldRejectNullRequestWithoutDelegatingToFlow() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        DefaultAskDataService service = new DefaultAskDataService(flowExecutor);

        AskDataResult result = service.ask(null);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, result.getErrorCode());
        assertEquals(0, flowExecutor.calls);
    }

    @Test
    public void shouldRejectBlankQuestionWithoutDelegatingToFlow() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        DefaultAskDataService service = new DefaultAskDataService(flowExecutor);
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("  ");

        AskDataResult result = service.ask(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, result.getErrorCode());
        assertEquals(0, flowExecutor.calls);
    }

    @Test
    public void shouldReturnSuccessfulResultFromFlow() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        AskDataResult expected = new AskDataResult();
        expected.setSuccess(true);
        expected.setSql("select count(*) from orders limit 100");
        flowExecutor.result = expected;
        DefaultAskDataService service = new DefaultAskDataService(flowExecutor);
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单数量");

        AskDataResult result = service.ask(request);

        assertTrue(result.isSuccess());
        assertEquals("select count(*) from orders limit 100", result.getSql());
        assertSame(expected, result);
        assertEquals(1, flowExecutor.calls);
    }

    @Test
    public void shouldReturnFailureResultFromFlow() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        AskDataResult expected = AskDataResult.failure(AskDataErrorCode.SQL_REJECTED, "only select allowed");
        flowExecutor.result = expected;
        DefaultAskDataService service = new DefaultAskDataService(flowExecutor);
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("删除订单");

        AskDataResult result = service.ask(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertSame(expected, result);
        assertEquals(1, flowExecutor.calls);
    }

    @Test
    public void shouldAdaptLegacySuccessToCoreResult() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        AskDataResult legacyResult = new AskDataResult();
        legacyResult.setSuccess(true);
        legacyResult.setBrief("订单总数为 10");
        legacyResult.setActualModelKey("provider-fallback");
        legacyResult.setModelAttempts(Arrays.asList("primary", "fallback"));
        flowExecutor.result = legacyResult;
        CoreAskDataServiceAdapter service = new CoreAskDataServiceAdapter(new DefaultAskDataService(flowExecutor));
        AskDataProjectContext projectContext = new AskDataProjectContext("project-a", "operator-a");

        com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult result = service.ask(
                new com.xiongx9527.fw.plus.ai.askdata.api.AskDataRequest(
                        projectContext, "订单数量"));

        assertTrue(result.isSuccess());
        assertEquals("订单总数为 10", result.getAnswer());
        assertEquals("provider-fallback", result.getActualModelKey());
        assertEquals(Arrays.asList("primary", "fallback"), result.getModelAttempts());
        assertEquals(1, flowExecutor.calls);
        assertSame(projectContext, flowExecutor.request.getProjectContext());
        assertEquals("project-a", flowExecutor.request.getProjectContext().getProjectId());
        assertEquals("operator-a", flowExecutor.request.getProjectContext().getOperatorId());
    }

    @Test
    public void shouldAdaptLegacyFailureToCoreError() {
        RecordingFlowExecutor flowExecutor = new RecordingFlowExecutor();
        flowExecutor.result = AskDataResult.failure(AskDataErrorCode.SQL_REJECTED,
                "JDBC execute failed: select * from confidential_orders");
        CoreAskDataServiceAdapter service = new CoreAskDataServiceAdapter(new DefaultAskDataService(flowExecutor));

        com.xiongx9527.fw.plus.ai.askdata.api.AskDataResult result = service.ask(
                new com.xiongx9527.fw.plus.ai.askdata.api.AskDataRequest(
                        new AskDataProjectContext("project-a", "operator-a"), "删除订单"));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getError().getCode());
        assertEquals("问数执行失败，请查看监控日志", result.getError().getMessage());
        assertFalse(result.getError().getMessage().contains("JDBC"));
        assertFalse(result.getError().getMessage().contains("confidential_orders"));
        assertEquals(1, flowExecutor.calls);
    }

    private static class RecordingFlowExecutor implements AskDataFlowExecutor {

        private int calls;
        private AskDataResult result;
        private AskDataRequest request;

        @Override
        public AskDataResult execute(AskDataRequest request) {
            calls++;
            this.request = request;
            return result;
        }
    }
}
