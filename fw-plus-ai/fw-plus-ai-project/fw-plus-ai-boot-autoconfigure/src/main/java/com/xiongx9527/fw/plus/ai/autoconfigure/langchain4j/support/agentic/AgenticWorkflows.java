package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.model.chat.ChatModel;

import java.util.function.Predicate;

/**
 * langchain4j-agentic 能力的统一门面（隔离层）：sequence / loop / conditional / parallel /
 * parallelMapper / supervisor / planner / humanInTheLoop / 单 agent（typed / untyped）/
 * 声明式 createAgenticSystem / A2A 构建器与 graph 入口；
 * 以及跨切面辅助：{@link #agenticToolProvider}（agent-as-tool）、
 * {@link #intentRouting}（意图条件路由，一次编排只判一次意图）。
 *
 * <p>业务与 magic 脚本只接触本类与其 builder/句柄，不直接依赖实验性的 agentic 原生 API；
 * 必要时本类可退化为手写编排而不影响使用方。门面无状态、不依赖 Spring 容器，
 * 经 {@link #instance()} 供脚本模块取用。</p>
 *
 * <p>编排产物（{@link UntypedAgent}）经 {@link AgenticToolProviders#agenticToolProvider(...)}
 * 包装为 {@code ToolProvider} 后，可用 {@code AssistantBuilder.toolProvider(...)} 挂入助手。</p>
 *
 * <p>编排入口均提供 typed 重载（{@code sequence(Class)} / {@code loop(Class)} 等）：
 * 传入带 {@code @Agent} 注解的接口，产物为该接口的代理；接口方法返回
 * {@link dev.langchain4j.service.TokenStream} 且子 agent 配置流式模型时，
 * 最后一步的流式响应会透传给调用方（编排流式）。</p>
 */
public final class AgenticWorkflows {

    private static final AgenticWorkflows INSTANCE = new AgenticWorkflows();

    private AgenticWorkflows() {
    }

    /** 供 magic 脚本模块取用的门面实例（无状态）。 */
    public static AgenticWorkflows instance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------
    // 编排：sequence / loop / conditional / parallel
    // ------------------------------------------------------------------

    /** 顺序编排（untyped）：subAgents 依次执行，输出经 AgenticScope 按 @V 变量名流向下一个 agent。 */
    public static AgenticSequenceBuilder<UntypedAgent> sequence() {
        return new AgenticSequenceBuilder<>(AgenticServices.sequenceBuilder());
    }

    /**
     * 顺序编排（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的代理。
     * 接口方法返回 {@link dev.langchain4j.service.TokenStream} 时整体可流式
     * （前提：最后一个子 agent 配流式模型且返回 TokenStream）。
     */
    public static <T> AgenticSequenceBuilder<T> sequence(Class<T> agentServiceClass) {
        return new AgenticSequenceBuilder<>(AgenticServices.sequenceBuilder(agentServiceClass));
    }

    /** 序列编排（流式模式）：预设 outputKey 为 "story"，便于快速配置流式场景。 */
    public static <T> AgenticSequenceBuilder<T> sequenceStreaming(Class<T> agentServiceClass) {
        return new AgenticSequenceBuilder<>(AgenticServices.sequenceBuilder(agentServiceClass)).outputKey("story");
    }

    /** 循环编排（untyped）：反复执行 subAgents，直到 exitCondition 满足或达到 maxIterations。 */
    public static AgenticLoopBuilder<UntypedAgent> loop() {
        return new AgenticLoopBuilder<>(AgenticServices.loopBuilder());
    }

    /** 循环编排（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的代理。 */
    public static <T> AgenticLoopBuilder<T> loop(Class<T> agentServiceClass) {
        return new AgenticLoopBuilder<>(AgenticServices.loopBuilder(agentServiceClass));
    }

    /** 条件编排（untyped）：按 AgenticScope 状态路由到满足条件的 agent 分支。 */
    public static AgenticConditionalBuilder<UntypedAgent> conditional() {
        return new AgenticConditionalBuilder<>(AgenticServices.conditionalBuilder());
    }

    /** 条件编排（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的代理。 */
    public static <T> AgenticConditionalBuilder<T> conditional(Class<T> agentServiceClass) {
        return new AgenticConditionalBuilder<>(AgenticServices.conditionalBuilder(agentServiceClass));
    }

    /** 并行编排（untyped）：多个独立 agent 同时执行（fan-out），结果按各自 outputKey 写回。 */
    public static AgenticParallelBuilder<UntypedAgent> parallel() {
        return new AgenticParallelBuilder<>(AgenticServices.parallelBuilder());
    }

    /** 并行编排（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的代理。 */
    public static <T> AgenticParallelBuilder<T> parallel(Class<T> agentServiceClass) {
        return new AgenticParallelBuilder<>(AgenticServices.parallelBuilder(agentServiceClass));
    }

    /** 并行映射编排：对 itemsProvider 指定的列表逐项并行执行映射 agent，结果列表汇聚写回。 */
    public static AgenticParallelMapperBuilder parallelMapper() {
        return new AgenticParallelMapperBuilder(AgenticServices.parallelMapperBuilder());
    }

    // ------------------------------------------------------------------
    // LLM 自主编排：supervisor / planner
    // ------------------------------------------------------------------

    /** 监督者编排：监督者 LLM 依据请求自主选择并调用 subAgents，产物为 {@link AgenticSupervisor} 句柄。 */
    public static AgenticSupervisorBuilder supervisor() {
        return new AgenticSupervisorBuilder(AgenticServices.supervisorBuilder());
    }

    /** Planner 编排：LLM Planner 依据 AgenticSystemTopology 自主编排 agents 执行，产物为 {@link UntypedAgent}。 */
    public static AgenticPlannerBuilder planner() {
        return new AgenticPlannerBuilder(AgenticServices.supervisorBuilder(UntypedAgent.class));
    }

    /**
     * 自定义规划器编排（untyped）：注入用户自定义的 {@link dev.langchain4j.agentic.planner.Planner}，
     * 产物为 {@link UntypedAgent} 句柄。不依赖 ChatModel 做调度决策。
     */
    public static AgenticPlannerBasedBuilder<UntypedAgent> plannerBuilder() {
        return new AgenticPlannerBasedBuilder<>(AgenticServices.plannerBuilder());
    }

    /**
     * 自定义规划器编排（typed）：传入带 {@code @Agent} 注解的接口，注入用户自定义的
     * {@link dev.langchain4j.agentic.planner.Planner}，产物为该接口的代理。
     */
    public static <T> AgenticPlannerBasedBuilder<T> plannerBuilder(Class<T> agentServiceClass) {
        return new AgenticPlannerBasedBuilder<>(AgenticServices.plannerBuilder(agentServiceClass));
    }

    // ------------------------------------------------------------------
    // 人工介入
    // ------------------------------------------------------------------

    /** 人工介入节点：编排执行到该节点时采用外部提供的 responseProvider 响应继续流转。 */
    public static AgenticHumanInTheLoopBuilder humanInTheLoop() {
        return new AgenticHumanInTheLoopBuilder();
    }

    // ------------------------------------------------------------------
    // 单 agent（typed / untyped）
    // ------------------------------------------------------------------

    /** 单 agent（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的可执行代理。 */
    public static <T> AgenticTypedAgentBuilder<T> agent(Class<T> agentServiceClass) {
        return new AgenticTypedAgentBuilder<>(AgenticServices.agentBuilder(agentServiceClass));
    }

    /** 单 agent（untyped）：产物为 {@link UntypedAgent} 句柄。 */
    public static AgenticUntypedAgentBuilder agent() {
        return new AgenticUntypedAgentBuilder(AgenticServices.agentBuilder());
    }

    /** 单 agent（typed）构建器入口（与 {@link #agent(Class)} 等价，供脚本统一命名）。 */
    public static <T> AgenticTypedAgentBuilder<T> agentBuilder(Class<T> agentServiceClass) {
        return agent(agentServiceClass);
    }

    /** 单 agent（untyped）构建器入口（与 {@link #agent()} 等价，供脚本统一命名）。 */
    public static AgenticUntypedAgentBuilder agentBuilder() {
        return agent();
    }

    // ------------------------------------------------------------------
    // 声明式系统 / A2A
    // ------------------------------------------------------------------

    /** 声明式 agent 系统：基于 {@code @SequenceAgent / @LoopAgent / @ConditionalAgent / @ParallelAgent} 等注解装配。 */
    public static <T> T createAgenticSystem(Class<T> agenticSystemClass) {
        return AgenticServices.createAgenticSystem(agenticSystemClass);
    }

    /** 声明式 agent 系统（指定共享 ChatModel）。 */
    public static <T> T createAgenticSystem(Class<T> agenticSystemClass, ChatModel chatModel) {
        return AgenticServices.createAgenticSystem(agenticSystemClass, chatModel);
    }

    /** A2A 远程 agent 客户端（untyped）：指定服务端地址，产物为 {@link UntypedAgent} 句柄。 */
    public static AgenticA2ABuilder a2a(String url) {
        return new AgenticA2ABuilder(url);
    }

    /** A2A 远程 agent 客户端（untyped）构建器入口（与 {@link #a2a(String)} 等价）。 */
    public static AgenticA2ABuilder a2aBuilder(String url) {
        return new AgenticA2ABuilder(url);
    }

    /** A2A 远程 agent 客户端（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的远程代理。 */
    public static <T> AgenticA2ATypedBuilder<T> a2a(String url, Class<T> agentServiceClass) {
        return new AgenticA2ATypedBuilder<>(url, agentServiceClass);
    }

    // ------------------------------------------------------------------
    // graph / agent-as-tool
    // ------------------------------------------------------------------

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

    /**
     * 意图条件路由入口：把意图分类接到条件编排（{@link #conditional()}），让「按意图选分支」
     * 在一次编排内只判一次意图。等价于 {@code AgenticIntentRouting.of(classifier)}。
     *
     * @see AgenticIntentRouting
     */
    public static <I extends Enum<I>> AgenticIntentRouting<I> intentRouting(IntentClassifier<I> classifier) {
        return AgenticIntentRouting.of(classifier);
    }

    // ------------------------------------------------------------------
    // 内部构建器（隔离层）
    // ------------------------------------------------------------------

    /** 顺序编排构建器（隔离层）：typed/untyped 共用，产物类型由 T 决定。 */
    public static final class AgenticSequenceBuilder<T> {

        private final SequentialAgentService<T> delegate;

        private AgenticSequenceBuilder(SequentialAgentService<T> delegate) {
            this.delegate = delegate;
        }

        public AgenticSequenceBuilder<T> subAgents(Object... agents) {
            delegate.subAgents(AgenticAgents.unwrapAll(agents));
            return this;
        }

        public AgenticSequenceBuilder<T> name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticSequenceBuilder<T> description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticSequenceBuilder<T> outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticSequenceBuilder<T> errorHandler(
                java.util.function.Function<dev.langchain4j.agentic.agent.ErrorContext,
                        dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
            delegate.errorHandler(errorHandler);
            return this;
        }

        public AgenticSequenceBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public T build() {
            return delegate.build();
        }
    }

    /** 循环编排构建器（隔离层）：typed/untyped 共用，产物类型由 T 决定。 */
    public static final class AgenticLoopBuilder<T> {

        private final LoopAgentService<T> delegate;

        private AgenticLoopBuilder(LoopAgentService<T> delegate) {
            this.delegate = delegate;
        }

        public AgenticLoopBuilder<T> subAgents(Object... agents) {
            delegate.subAgents(AgenticAgents.unwrapAll(agents));
            return this;
        }

        public AgenticLoopBuilder<T> name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticLoopBuilder<T> description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticLoopBuilder<T> outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticLoopBuilder<T> maxIterations(int maxIterations) {
            delegate.maxIterations(maxIterations);
            return this;
        }

        public AgenticLoopBuilder<T> exitCondition(Predicate<AgenticScope> condition) {
            delegate.exitCondition(condition);
            return this;
        }

        public AgenticLoopBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public T build() {
            return delegate.build();
        }
    }

    /** 条件编排构建器（隔离层）：typed/untyped 共用，产物类型由 T 决定；subAgents 以「条件谓词 + agent」成对声明。 */
    public static final class AgenticConditionalBuilder<T> {

        private final ConditionalAgentService<T> delegate;

        private AgenticConditionalBuilder(ConditionalAgentService<T> delegate) {
            this.delegate = delegate;
        }

        /** 声明一个分支：条件满足（基于 AgenticScope 状态）时该 agent 参与执行。 */
        public AgenticConditionalBuilder<T> subAgents(Predicate<AgenticScope> condition,
                Object agent) {
            delegate.subAgents(condition, AgenticAgents.unwrap(agent));
            return this;
        }

        public AgenticConditionalBuilder<T> name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticConditionalBuilder<T> description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticConditionalBuilder<T> outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticConditionalBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public T build() {
            return delegate.build();
        }
    }

    /** 并行编排构建器（隔离层）：typed/untyped 共用，产物类型由 T 决定。 */
    public static final class AgenticParallelBuilder<T> {

        private final ParallelAgentService<T> delegate;

        private AgenticParallelBuilder(ParallelAgentService<T> delegate) {
            this.delegate = delegate;
        }

        public AgenticParallelBuilder<T> subAgents(Object... agents) {
            delegate.subAgents(AgenticAgents.unwrapAll(agents));
            return this;
        }

        public AgenticParallelBuilder<T> name(String name) {
            delegate.name(name);
            return this;
        }

        public AgenticParallelBuilder<T> description(String description) {
            delegate.description(description);
            return this;
        }

        public AgenticParallelBuilder<T> outputKey(String outputKey) {
            delegate.outputKey(outputKey);
            return this;
        }

        public AgenticParallelBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
            delegate.listener(listener);
            return this;
        }

        public T build() {
            return delegate.build();
        }
    }
}