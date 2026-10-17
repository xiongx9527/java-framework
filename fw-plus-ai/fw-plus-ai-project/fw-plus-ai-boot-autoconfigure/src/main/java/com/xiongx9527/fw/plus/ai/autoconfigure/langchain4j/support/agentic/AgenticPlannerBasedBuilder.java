package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlannerBasedService;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 自定义规划器编排构建器（隔离层）：包装 {@link PlannerBasedService}，允许注入
 * 用户自定义的 {@link Planner} 实现（如顺序执行、目标导向、对等网络等）。
 *
 * <p>与 {@link AgenticPlannerBuilder}（LLM 决策式）不同，本类由调用方提供规划逻辑，
 * 不依赖 ChatModel 做调度决策。</p>
 *
 * @param <T> 调用方声明的 agent 接口类型
 */
public final class AgenticPlannerBasedBuilder<T> {

    private final PlannerBasedService<T> delegate;
    private Supplier<Planner> plannerSupplier;
    private AgenticScopeStore scopeStore;

    AgenticPlannerBasedBuilder(PlannerBasedService<T> delegate) {
        this.delegate = delegate;
    }

    /** 注入自定义规划器（决定子 agent 的调用顺序与条件）。 */
    public AgenticPlannerBasedBuilder<T> planner(Supplier<Planner> plannerSupplier) {
        this.plannerSupplier = plannerSupplier;
        return this;
    }

    public AgenticPlannerBasedBuilder<T> subAgents(Object... agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticPlannerBasedBuilder<T> subAgents(Collection<?> agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticPlannerBasedBuilder<T> name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> output(Function<dev.langchain4j.agentic.scope.AgenticScope, Object> output) {
        delegate.output(output);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> beforeCall(Consumer<dev.langchain4j.agentic.scope.AgenticScope> beforeCall) {
        delegate.beforeCall(beforeCall);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> compensateOnError(boolean compensateOnError) {
        delegate.compensateOnError(compensateOnError);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> errorHandler(
            Function<dev.langchain4j.agentic.agent.ErrorContext,
                    dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
        delegate.errorHandler(errorHandler);
        return this;
    }

    public AgenticPlannerBasedBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    /**
     * agentic 原生存储：把编排共享状态（{@code AgenticScope}）持久化到给定
     * {@link AgenticScopeStore}（构建时安装为全局 {@code AgenticScopePersister}，进程级生效）。
     *
     * <p>Spring 环境缺省已由 scope-store 自动配置统一安装，无需在此显式调用。</p>
     */
    public AgenticPlannerBasedBuilder<T> scopeStore(AgenticScopeStore scopeStore) {
        this.scopeStore = scopeStore;
        return this;
    }

    public T build() {
        if (scopeStore != null) {
            AgenticScopePersister.setStore(scopeStore);
        }
        if (plannerSupplier != null) {
            delegate.planner(plannerSupplier);
        }
        return delegate.build();
    }
}
