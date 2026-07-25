package com.xiongx9527.fw.plus.ai.askdata.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SequentialAskDataAgentDispatcher implements AskDataAgentDispatcher {

    private final List<AskDataSubAgent> defaultSubAgents;

    public SequentialAskDataAgentDispatcher() {
        this(Collections.<AskDataSubAgent>emptyList());
    }

    public SequentialAskDataAgentDispatcher(List<AskDataSubAgent> defaultSubAgents) {
        this.defaultSubAgents = defaultSubAgents == null ? Collections.<AskDataSubAgent>emptyList()
                : new ArrayList<AskDataSubAgent>(defaultSubAgents);
    }

    @Override
    public AskDataDispatchResult dispatch(AskDataDispatchRequest request) {
        AskDataDispatchResult dispatchResult = new AskDataDispatchResult();
        if (request == null) {
            dispatchResult.setSuccess(false);
            return dispatchResult;
        }
        List<AskDataSubAgent> subAgents = request.getSubAgents().isEmpty() ? defaultSubAgents : request.getSubAgents();
        for (AskDataSubAgent subAgent : subAgents) {
            AskDataSubAgentResult result = executeSubAgent(subAgent, request);
            dispatchResult.addResult(result);
            if (!result.isSuccess()) {
                if (request.getFailurePolicy() == AskDataDispatchRequest.FailurePolicy.DEGRADE) {
                    result.setSuccess(true);
                    result.setOutputSummary(request.getDegradedOutputSummary());
                } else {
                    dispatchResult.setSuccess(false);
                    if (request.getFailurePolicy() == AskDataDispatchRequest.FailurePolicy.TERMINATE) {
                        break;
                    }
                }
            }
        }
        return dispatchResult;
    }

    private AskDataSubAgentResult executeSubAgent(AskDataSubAgent subAgent, AskDataDispatchRequest request) {
        long startMillis = System.currentTimeMillis();
        AskDataSubAgentResult result;
        String name = subAgent == null ? null : subAgent.getName();
        try {
            if (subAgent == null) {
                result = AskDataSubAgentResult.failure("Sub-agent must not be null");
            } else {
                result = subAgent.executeSubTask(new AskDataSubAgentRequest(name, request.getContext(),
                        request.getTimeoutMillis()));
                if (result == null) {
                    result = AskDataSubAgentResult.failure("Sub-agent returned no result");
                }
            }
        } catch (RuntimeException ex) {
            result = AskDataSubAgentResult.failure(ex.getMessage());
        }
        long endMillis = System.currentTimeMillis();
        result.setAgentName(name);
        result.setStartMillis(startMillis);
        result.setEndMillis(endMillis);
        if (request.getTimeoutMillis() > 0 && endMillis - startMillis > request.getTimeoutMillis()) {
            result.setTimedOut(true);
            result.setSuccess(false);
            result.setErrorMessage("Sub-agent timeout: " + name);
        }
        return result;
    }
}
