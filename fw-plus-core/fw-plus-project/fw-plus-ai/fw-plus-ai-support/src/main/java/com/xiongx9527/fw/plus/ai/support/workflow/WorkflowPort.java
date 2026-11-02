package com.xiongx9527.fw.plus.ai.support.workflow;

/**
 * 复杂任务编排入口（agentic 端口）。默认实现直接返回 unsupported，{@code tool.mode=none} 时永不调用。
 *
 * <p>出处（统一抽象，背后可换实现）：
 * <ul>
 *   <li>AgentScope：{@code io.agentscope} 的 AgenticScope / sequenceBuilder / loopBuilder / plannerBuilder / supervisorBuilder / humanInTheLoopBuilder / AgentListener</li>
 *   <li>langchain4j：{@code dev.langchain4j.agentic}（beta，从未 GA；Java 17 字节码）</li>
 *   <li>Spring AI：agentic（Observation / Graph）</li>
 * </ul>
 *
 * <p>端口只出现 {@code goal} / {@code step} / {@code budget}，不出现 Agent / Scope / Supervisor——换实现不改端口。
 * 拓扑由具体 backend 的 adapter 内部组装。
 */
public interface WorkflowPort {
    default WorkflowResult run(WorkflowRequest request, WorkflowSink sink) { throw new UnsupportedOperationException(); }
}
