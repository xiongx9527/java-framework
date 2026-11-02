package com.xiongx9527.fw.plus.ai.support.workflow;

/** 编排预算硬上限（防止 supervisor/planner 失控烧配额）。出处：AgentScope 无内置上限，需 adapter 强制终止；langchain4j-agentic / spring-ai 同样需要。 */
public class WorkflowBudget {
    private final int maxSteps;
    private final long maxTokens;
    private final long maxDurationMs;
    public WorkflowBudget(int maxSteps, long maxTokens, long maxDurationMs) {
        this.maxSteps = maxSteps; this.maxTokens = maxTokens; this.maxDurationMs = maxDurationMs;
    }
    public int maxSteps() { return maxSteps; }
    public long maxTokens() { return maxTokens; }
    public long maxDurationMs() { return maxDurationMs; }
    public static WorkflowBudget of(int steps, long tokens, long ms) { return new WorkflowBudget(steps, tokens, ms); }
}
