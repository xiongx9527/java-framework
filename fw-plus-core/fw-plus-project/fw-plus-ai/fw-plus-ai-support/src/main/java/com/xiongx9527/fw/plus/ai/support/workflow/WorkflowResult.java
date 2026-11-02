package com.xiongx9527.fw.plus.ai.support.workflow;

import java.util.Collections;
import java.util.List;

/** 编排结果。出处：langchain4j-agentic / AgentScope 的 workflow 输出；spring-ai 的 final answer。 */
public class WorkflowResult {
    private final String answer;
    private final List<WorkflowStep> steps;
    private final String stopReason;
    public WorkflowResult(String answer, List<WorkflowStep> steps, String stopReason) {
        this.answer = answer;
        this.steps = steps == null ? Collections.<WorkflowStep>emptyList() : steps;
        this.stopReason = stopReason;
    }
    public String answer() { return answer; }
    public List<WorkflowStep> steps() { return steps; }
    public String stopReason() { return stopReason; }
}
