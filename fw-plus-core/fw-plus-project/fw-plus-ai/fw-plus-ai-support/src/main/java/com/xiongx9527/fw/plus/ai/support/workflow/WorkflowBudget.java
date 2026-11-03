package com.xiongx9527.fw.plus.ai.support.workflow;

/**
 * 编排预算：步数 / token / 时间三道上限，任一触顶即停止。
 */
public final class WorkflowBudget {

    private final int maxSteps;

    private final long maxTokens;

    private final long maxMillis;

    public WorkflowBudget(int maxSteps, long maxTokens, long maxMillis) {
        this.maxSteps = maxSteps;
        this.maxTokens = maxTokens;
        this.maxMillis = maxMillis;
    }

    public static WorkflowBudget defaults() {
        return new WorkflowBudget(8, 32000L, 120000L);
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public long getMaxTokens() {
        return maxTokens;
    }

    public long getMaxMillis() {
        return maxMillis;
    }
}
