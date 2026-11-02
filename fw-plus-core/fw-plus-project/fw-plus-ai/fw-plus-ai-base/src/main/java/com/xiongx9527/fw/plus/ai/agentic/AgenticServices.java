package com.xiongx9527.fw.plus.ai.agentic;

import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;

/**
 * agentic 静态工厂，镜像 langchain4j 1.22/1.23 的 {@code dev.langchain4j.agentic.AgenticServices}
 * （静态 builder 方法：agentBuilder / sequenceBuilder / parallelBuilder / parallelMapperBuilder /
 * loopBuilder / conditionalBuilder / supervisorBuilder / plannerBuilder / humanInTheLoopBuilder / a2aBuilder /
 * createAgenticSystem）。
 *
 * <p>出处：langchain4j-agentic（beta，Java 17）/ Spring AI agentic / <b>AgentScope</b>（AgenticScope +
 * sequenceBuilder / loopBuilder / plannerBuilder / supervisorBuilder / humanInTheLoopBuilder / AgentListener / a2aBuilder）。
 *
 * <p>base 只提供官方形状的契约；具体构建由 {@code fw-plus-ai-agentscope} 等后端实现（base 静态方法默认抛
 * UnsupportedOperationException，避免自研一套编排实现）。
 */
public final class AgenticServices {

    private AgenticServices() {}

    public static AgentBuilder<UntypedAgent> agentBuilder() { return unsupported(); }

    public static <T> AgentBuilder<T> agentBuilder(Class<T> agentClass) { return unsupported(); }

    public static HumanInTheLoopBuilder humanInTheLoopBuilder() { return unsupported(); }

    public static SequentialAgentService<UntypedAgent> sequenceBuilder() { return unsupported(); }

    public static <T> SequentialAgentService<T> sequenceBuilder(Class<T> agentClass) { return unsupported(); }

    public static ParallelAgentService<UntypedAgent> parallelBuilder() { return unsupported(); }

    public static <T> ParallelAgentService<T> parallelBuilder(Class<T> agentClass) { return unsupported(); }

    public static ParallelAgentService<UntypedAgent> parallelMapperBuilder() { return unsupported(); }

    public static <T> ParallelAgentService<T> parallelMapperBuilder(Class<T> agentClass) { return unsupported(); }

    public static LoopAgentService<UntypedAgent> loopBuilder() { return unsupported(); }

    public static <T> LoopAgentService<T> loopBuilder(Class<T> agentClass) { return unsupported(); }

    public static ConditionalAgentService<UntypedAgent> conditionalBuilder() { return unsupported(); }

    public static <T> ConditionalAgentService<T> conditionalBuilder(Class<T> agentClass) { return unsupported(); }

    public static SupervisorAgentService<UntypedAgent> supervisorBuilder() { return unsupported(); }

    public static <T> SupervisorAgentService<T> supervisorBuilder(Class<T> agentClass) { return unsupported(); }

    public static PlannerBasedService<UntypedAgent> plannerBuilder() { return unsupported(); }

    public static <T> PlannerBasedService<T> plannerBuilder(Class<T> agentClass) { return unsupported(); }

    public static A2AClientBuilder<UntypedAgent> a2aBuilder(String url) { return unsupported(); }

    public static <T> A2AClientBuilder<T> a2aBuilder(String url, Class<T> agentClass) { return unsupported(); }

    public static <T> T createAgenticSystem(Class<T> agenticSystemClass) { return unsupported(); }

    public static <T> T createAgenticSystem(Class<T> agenticSystemClass, ChatModel model) { return unsupported(); }

    private static <R> R unsupported() {
        throw new UnsupportedOperationException(
                "AgenticServices 的静态构建器由具体后端实现（fw-plus-ai-agentscope / langchain4j-agentic / spring-ai）；base 只提供契约形状");
    }
}
