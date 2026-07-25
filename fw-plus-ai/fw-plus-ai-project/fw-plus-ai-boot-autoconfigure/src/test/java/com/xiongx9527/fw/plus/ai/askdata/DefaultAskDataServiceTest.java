package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.flow.AskDataFlowExecutor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

    private static class RecordingFlowExecutor implements AskDataFlowExecutor {

        private int calls;
        private AskDataResult result;

        @Override
        public AskDataResult execute(AskDataRequest request) {
            calls++;
            return result;
        }
    }
}
