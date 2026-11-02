package com.xiongx9527.fw.plus.ai.support.workflow;

/** 单步执行记录。出处：AgentScope {@code AgentListener.before/afterAgentInvocation}；langchain4j-agentic step；spring-ai node。 */
public class WorkflowStep {
    private final String name;
    private final String toolName;
    private final String resultSummary;
    private final long elapsedMs;
    public WorkflowStep(String name, String toolName, String resultSummary, long elapsedMs) {
        this.name = name; this.toolName = toolName; this.resultSummary = resultSummary; this.elapsedMs = elapsedMs;
    }
    public String name() { return name; }
    public String toolName() { return toolName; }
    public String resultSummary() { return resultSummary; }
    public long elapsedMs() { return elapsedMs; }
}
