package com.xiongx9527.fw.plus.ai.askdata.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataDispatchRequest {

    public enum FailurePolicy {
        CONTINUE,
        DEGRADE,
        TERMINATE
    }

    private final List<AskDataSubAgent> subAgents;
    private final AskDataStepContext context;
    private final long timeoutMillis;
    private final FailurePolicy failurePolicy;
    private final String degradedOutputSummary;

    public AskDataDispatchRequest(List<AskDataSubAgent> subAgents, AskDataStepContext context, long timeoutMillis,
            FailurePolicy failurePolicy, String degradedOutputSummary) {
        this.subAgents = subAgents == null ? Collections.<AskDataSubAgent>emptyList()
                : Collections.unmodifiableList(new ArrayList<AskDataSubAgent>(subAgents));
        this.context = context;
        this.timeoutMillis = timeoutMillis;
        this.failurePolicy = failurePolicy == null ? FailurePolicy.TERMINATE : failurePolicy;
        this.degradedOutputSummary = degradedOutputSummary;
    }

    public List<AskDataSubAgent> getSubAgents() {
        return subAgents;
    }

    public AskDataStepContext getContext() {
        return context;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public FailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public String getDegradedOutputSummary() {
        return degradedOutputSummary;
    }
}
