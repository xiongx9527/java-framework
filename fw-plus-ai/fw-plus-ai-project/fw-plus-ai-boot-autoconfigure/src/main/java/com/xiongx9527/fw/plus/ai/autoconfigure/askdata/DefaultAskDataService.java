package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataFlowExecutor;

public class DefaultAskDataService implements AskDataService {

    private final AskDataFlowExecutor flowExecutor;

    public DefaultAskDataService(AskDataFlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    @Override
    public AskDataResult ask(AskDataRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return AskDataResult.failure(AskDataErrorCode.MODEL_RESPONSE_INVALID, "question must not be blank");
        }
        return flowExecutor.execute(request);
    }
}
