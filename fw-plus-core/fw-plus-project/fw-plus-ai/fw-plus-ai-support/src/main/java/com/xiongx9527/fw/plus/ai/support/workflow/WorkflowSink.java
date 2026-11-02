package com.xiongx9527.fw.plus.ai.support.workflow;

/** 编排过程进度回调（逐 step 上报）。出处：AgentScope {@code AgentListener} 观测；langchain4j-agentic 同理。 */
public interface WorkflowSink {
    void onStep(WorkflowStep step);
}
