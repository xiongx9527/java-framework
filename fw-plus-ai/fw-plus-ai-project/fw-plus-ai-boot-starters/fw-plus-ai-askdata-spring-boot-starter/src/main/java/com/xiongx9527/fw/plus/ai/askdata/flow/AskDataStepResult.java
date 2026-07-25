package com.xiongx9527.fw.plus.ai.askdata.flow;

public class AskDataStepResult {

    private final boolean success;
    private final String outputSummary;
    private final String errorMessage;

    private AskDataStepResult(boolean success, String outputSummary, String errorMessage) {
        this.success = success;
        this.outputSummary = outputSummary;
        this.errorMessage = errorMessage;
    }

    public static AskDataStepResult success(String outputSummary) {
        return new AskDataStepResult(true, outputSummary, null);
    }

    public static AskDataStepResult failure(String errorMessage) {
        return new AskDataStepResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
