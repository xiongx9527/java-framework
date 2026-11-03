package com.xiongx9527.fw.plus.ai.support.workflow;

/**
 * 一个编排步骤的执行记录。
 */
public final class WorkflowStep {

    public enum Status {
        RUNNING,
        DONE,
        FAILED
    }

    private final String name;

    private final Status status;

    private final String output;

    public WorkflowStep(String name, Status status, String output) {
        this.name = name;
        this.status = status;
        this.output = output;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }
}
