package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

public class AskDataSubAgentRequest {

    private final String agentName;
    private final AskDataStepContext context;
    private final long timeoutMillis;

    public AskDataSubAgentRequest(String agentName, AskDataStepContext context, long timeoutMillis) {
        this.agentName = agentName;
        this.context = context;
        this.timeoutMillis = timeoutMillis;
    }

    public String getAgentName() {
        return agentName;
    }

    public AskDataStepContext getContext() {
        return context;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}
