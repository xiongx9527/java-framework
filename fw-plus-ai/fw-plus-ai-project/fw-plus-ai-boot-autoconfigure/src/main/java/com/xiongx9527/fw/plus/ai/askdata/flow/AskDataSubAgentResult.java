package com.xiongx9527.fw.plus.ai.askdata.flow;

public class AskDataSubAgentResult {

    private String agentName;
    private long startMillis;
    private long endMillis;
    private boolean timedOut;
    private boolean success;
    private String outputSummary;
    private String errorMessage;

    public static AskDataSubAgentResult success(String outputSummary) {
        AskDataSubAgentResult result = new AskDataSubAgentResult();
        result.success = true;
        result.outputSummary = outputSummary;
        return result;
    }

    public static AskDataSubAgentResult failure(String errorMessage) {
        AskDataSubAgentResult result = new AskDataSubAgentResult();
        result.errorMessage = errorMessage;
        return result;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
