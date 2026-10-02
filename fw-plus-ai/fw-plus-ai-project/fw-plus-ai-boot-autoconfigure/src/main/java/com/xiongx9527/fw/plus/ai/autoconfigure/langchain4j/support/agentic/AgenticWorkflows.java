package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;

import java.util.function.Predicate;

/**
 * langchain4j-agentic 基本能力的统一门面：sequence / loop / conditional / parallel 四种编排
 * 构建器与 graph 入口的隔离层。业务与脚本只接触本类与其 builder/句柄，不直接依赖
 * 实验性的 agentic 原生 API；必要时本类可退化为手写编排而不影响使用方。
 *
 * <p>编排产物（{@link UntypedAgent}）经 {@link AgenticToolProviders#agenticToolProvider(...)}
 * 包装为 {@code ToolProvider} 后，可用 {@code AssistantBuilder.toolProvider(...)} 挂入助手。</p>
 */
public final class AgenticWorkflows {

    private static final AgenticWorkflows INSTANCE = new AgenticWorkflows();

    private AgenticWorkflows() {
    }

    /** 供 magic 脚本模块取用的门面实例（无状态）。 */
    public static AgenticWorkflows instance() {
        return INSTANCE;
    }

    /** 顺序编排：subAgents 依次执行，输出经 AgenticScope 按 @V 变量名流向下一个 agent。 */
    public static AgenticSequenceBuilder sequence() {
        return new AgenticSequenceBuilder(AgenticServices.sequenceBuilder());
    }

    /** 循环编排：反复执行 subAgents，直到 exitCondition 满足或达到 maxIterations。 */
    public static AgenticLoopBuilder loop() {
        return new AgenticLoopBuilder(AgenticServices.loopBuilder());
    }

    /** 条件编排：按 AgenticScope 状态路由到满足条件的 agent 分支。 */
    public static AgenticConditionalBuilder conditional() {
        return new AgenticConditionalBuilder(AgenticServices.conditionalBuilder());
    }

    /** 并行编排：多个独立 agent 同时执行（fan-out），结果按各自 outputKey 写回。 */
    public static AgenticParallelBuilder parallel() {
        return new AgenticParallelBuilder(AgenticServices.parallelBuilder());
    }

    /**
     * graph 编排入口：基于 langgraph4j 的 StateGraph（节点/条件边），编译后经
     * {@link AgenticToolProviders#graphToolProvider} 挂为工具。
     *
     * @param stateFactory 业务的状态工厂（AgentState 子类）
     */
    public static <T extends org.bsc.langgraph4j.state.AgentState> org.bsc.langgraph4j.StateGraph<T> graph(
            org.bsc.langgraph4j.state.AgentStateFactory<T> stateFactory) {
        return new org.bsc.langgraph4j.StateGraph<>(stateFactory);
    }

    /** 把 agentic 编排产物包装为 ToolProvider（agent-as-tool），挂入助手 toolProvider。 */
    public static dev.langchain4j.service.tool.ToolProvider agenticToolProvider(UntypedAgent pipeline,
            String toolName, String toolDescription, String outputKey, String... inputKeys) {
        return AgenticToolProviders.agenticToolProvider(pipeline, toolName, toolDescription, outputKey, inputKeys);
    }

    /** 把 langgraph4j 编译图包装为 ToolProvider（agent-as-tool）。 */
    public static <T extends org.bsc.langgraph4j.state.AgentState> dev.langchain4j.service.tool.ToolProvider graphToolProvider(
            org.bsc.langgraph4j.CompiledGraph<T> compiledGraph, String toolName, String toolDescription) {
        return AgenticToolProviders.graphToolProvider(compiledGraph, toolName, toolDescription);
    }

    /** 顺序编排构建器（隔离层）。 */
    public static final class AgenticSequenceBuilder {

        private final SequentialAgentService<UntypedAgent> delegate;

        private AgenticSequenceBuilder(SequentialAgentService<UntypedAgent> delegate) {
            this.delegate = delegate;
        }

        public AgenticSequenceBuilder subAgents(Object... agents) {
            delegate.subAgents(agents);
            return this;
        }

        public AgenticSequenceBuilder name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticSequenceBuilder description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticSequenceBuilder outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticSequenceBuilder errorHandler(
                java.util.function.Function<dev.langchain4j.agentic.agent.ErrorContext,
                        dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
            delegate.errorHandler(errorHandler);
            return this;
        }

        public AgenticSequenceBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public UntypedAgent build() {
            return delegate.build();
        }
    }

    /** 循环编排构建器（隔离层）。 */
    public static final class AgenticLoopBuilder {

        private final LoopAgentService<UntypedAgent> delegate;

        private AgenticLoopBuilder(LoopAgentService<UntypedAgent> delegate) {
            this.delegate = delegate;
        }

        public AgenticLoopBuilder subAgents(Object... agents) {
            delegate.subAgents(agents);
            return this;
        }

        public AgenticLoopBuilder name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticLoopBuilder description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticLoopBuilder outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticLoopBuilder maxIterations(int maxIterations) {
            delegate.maxIterations(maxIterations);
            return this;
        }

        public AgenticLoopBuilder exitCondition(Predicate<dev.langchain4j.agentic.scope.AgenticScope> condition) {
            delegate.exitCondition(condition);
            return this;
        }

        public AgenticLoopBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public UntypedAgent build() {
            return delegate.build();
        }
    }

    /** 条件编排构建器（隔离层）：subAgents 以「条件谓词 + agent」成对声明。 */
    public static final class AgenticConditionalBuilder {

        private final ConditionalAgentService<UntypedAgent> delegate;

        private AgenticConditionalBuilder(ConditionalAgentService<UntypedAgent> delegate) {
            this.delegate = delegate;
        }

        /** 声明一个分支：条件满足（基于 AgenticScope 状态）时该 agent 参与执行。 */
        public AgenticConditionalBuilder subAgents(Predicate<dev.langchain4j.agentic.scope.AgenticScope> condition,
                Object agent) {
            delegate.subAgents(condition, agent);
            return this;
        }

        public AgenticConditionalBuilder name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticConditionalBuilder description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticConditionalBuilder outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticConditionalBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public UntypedAgent build() {
            return delegate.build();
        }
    }

    /** 并行编排构建器（隔离层）。 */
    public static final class AgenticParallelBuilder {

        private final ParallelAgentService<UntypedAgent> delegate;

        private AgenticParallelBuilder(ParallelAgentService<UntypedAgent> delegate) {
            this.delegate = delegate;
        }

        public AgenticParallelBuilder subAgents(Object... agents) {
            delegate.subAgents(agents);
            return this;
        }

        public AgenticParallelBuilder name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticParallelBuilder description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticParallelBuilder outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticParallelBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public UntypedAgent build() {
            return delegate.build();
        }
    }
}
