package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.supervisor.SupervisorAgentService;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Planner 编排构建器（隔离层）：由 LLM Planner 依据 AgenticSystemTopology 自主编排 agents 执行。
 *
 * <p>默认采用监督者式 LLM Planner，底层复用 langchain4j-agentic 官方的
 * {@code SupervisorAgentService}（其内部装配 {@code SupervisorPlanner}），因此 ChatModel 必须由
 * 调用方经 {@link #chatModel(ChatModel)} 显式注入。产物为 {@link UntypedAgent}，可独立执行，
 * 也可作为 subAgent 挂入其他编排。</p>
 */
public final class AgenticPlannerBuilder {

    private final SupervisorAgentService<UntypedAgent> delegate;

    private ChatModel chatModel;
    private ChatMemoryProvider chatMemoryProvider;
    private int maxAgentsInvocations = 10;
    private SupervisorContextStrategy contextGenerationStrategy = SupervisorContextStrategy.CHAT_MEMORY;
    private SupervisorResponseStrategy responseStrategy = SupervisorResponseStrategy.LAST;
    private Function<AgenticScope, String> requestGenerator;
    private String supervisorContext;
    private String outputKey;
    private Function<AgenticScope, Object> output;
    private Consumer<AgenticScope> beforeCall;
    private AgenticScopeStore scopeStore;

    AgenticPlannerBuilder(SupervisorAgentService<UntypedAgent> delegate) {
        this.delegate = delegate;
    }

    /** 注入 Planner 使用的 ChatModel（必填）。 */
    public AgenticPlannerBuilder chatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        return this;
    }

    public AgenticPlannerBuilder chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        this.chatMemoryProvider = chatMemoryProvider;
        return this;
    }

    public AgenticPlannerBuilder maxAgentsInvocations(int maxAgentsInvocations) {
        this.maxAgentsInvocations = maxAgentsInvocations;
        return this;
    }

    public AgenticPlannerBuilder contextGenerationStrategy(SupervisorContextStrategy contextGenerationStrategy) {
        this.contextGenerationStrategy = contextGenerationStrategy;
        return this;
    }

    public AgenticPlannerBuilder responseStrategy(SupervisorResponseStrategy responseStrategy) {
        this.responseStrategy = responseStrategy;
        return this;
    }

    public AgenticPlannerBuilder requestGenerator(Function<AgenticScope, String> requestGenerator) {
        this.requestGenerator = requestGenerator;
        return this;
    }

    /** 供 Planner 决策参考的监督者上下文（写入 AgenticScope 的 supervisorContext 状态）。 */
    public AgenticPlannerBuilder supervisorContext(String supervisorContext) {
        this.supervisorContext = supervisorContext;
        return this;
    }

    public AgenticPlannerBuilder subAgents(Object... agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticPlannerBuilder subAgents(Collection<?> agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticPlannerBuilder name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticPlannerBuilder description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticPlannerBuilder outputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    public AgenticPlannerBuilder output(Function<AgenticScope, Object> output) {
        this.output = output;
        return this;
    }

    public AgenticPlannerBuilder beforeCall(Consumer<AgenticScope> beforeCall) {
        this.beforeCall = beforeCall;
        return this;
    }

    public AgenticPlannerBuilder compensateOnError(boolean compensateOnError) {
        delegate.compensateOnError(compensateOnError);
        return this;
    }

    public AgenticPlannerBuilder errorHandler(
            Function<dev.langchain4j.agentic.agent.ErrorContext,
                    dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
        delegate.errorHandler(errorHandler);
        return this;
    }

    public AgenticPlannerBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    /**
     * agentic 原生存储：把编排共享状态（{@code AgenticScope}）持久化到给定
     * {@link AgenticScopeStore}（构建时安装为全局 {@code AgenticScopePersister}，进程级生效）。
     *
     * <p>Spring 环境缺省已由 scope-store 自动配置统一安装，无需在此显式调用。</p>
     */
    public AgenticPlannerBuilder scopeStore(AgenticScopeStore scopeStore) {
        this.scopeStore = scopeStore;
        return this;
    }

    public UntypedAgent build() {
        if (chatModel == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.ai.AgenticPlannerBuilder.chatModelRequired"));
        }
        if (scopeStore != null) {
            AgenticScopePersister.setStore(scopeStore);
        }
        delegate.chatModel(chatModel);
        if (chatMemoryProvider != null) {
            delegate.chatMemoryProvider(chatMemoryProvider);
        }
        delegate.maxAgentsInvocations(maxAgentsInvocations);
        delegate.contextGenerationStrategy(contextGenerationStrategy);
        delegate.responseStrategy(responseStrategy);
        if (requestGenerator != null) {
            delegate.requestGenerator(requestGenerator);
        }
        if (supervisorContext != null) {
            delegate.supervisorContext(supervisorContext);
        }
        if (outputKey != null) {
            delegate.outputKey(outputKey);
        }
        if (output != null) {
            delegate.output(output);
        }
        if (beforeCall != null) {
            delegate.beforeCall(beforeCall);
        }
        return delegate.build();
    }
}
