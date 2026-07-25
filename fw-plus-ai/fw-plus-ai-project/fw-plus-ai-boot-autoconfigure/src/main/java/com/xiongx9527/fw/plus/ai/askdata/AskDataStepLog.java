package com.xiongx9527.fw.plus.ai.askdata;

public class AskDataStepLog {

    private String stepKey;
    private String stepName;
    private boolean success;
    private String message;

    public AskDataStepLog() {
    }

    public AskDataStepLog(String stepKey, String stepName, boolean success, String message) {
        this.stepKey = stepKey;
        this.stepName = stepName;
        this.success = success;
        this.message = message;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
