package com.xiongx9527.fw.plus.ai.support.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编排结果：成功与否 + 产出 + 步骤轨迹。
 */
public final class WorkflowResult {

    private final boolean success;

    private final String output;

    private final String error;

    private final List<WorkflowStep> steps;

    private WorkflowResult(boolean success, String output, String error, List<WorkflowStep> steps) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.steps = Collections.unmodifiableList(new ArrayList<WorkflowStep>(steps));
    }

    public static WorkflowResult success(String output, List<WorkflowStep> steps) {
        return new WorkflowResult(true, output, null, steps == null ? new ArrayList<WorkflowStep>() : steps);
    }

    public static WorkflowResult failure(String error, List<WorkflowStep> steps) {
        return new WorkflowResult(false, null, error, steps == null ? new ArrayList<WorkflowStep>() : steps);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }
}
