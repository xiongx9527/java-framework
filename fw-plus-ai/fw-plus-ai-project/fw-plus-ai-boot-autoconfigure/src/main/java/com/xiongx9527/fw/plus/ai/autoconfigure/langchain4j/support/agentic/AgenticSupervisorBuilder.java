package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgentService;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 监督者编排构建器（隔离层）：监督者 LLM 依据请求自主选择并调用 subAgents，
 * 最终输出写回 outputKey。ChatModel 必须由调用方经 {@link #chatModel(ChatModel)} 显式注入。
 */
public final class AgenticSupervisorBuilder {

    private final SupervisorAgentService<SupervisorAgent> delegate;

    AgenticSupervisorBuilder(SupervisorAgentService<SupervisorAgent> delegate) {
        this.delegate = delegate;
    }

    /** 注入监督者使用的 ChatModel（必填）。 */
    public AgenticSupervisorBuilder chatModel(ChatModel chatModel) {
        delegate.chatModel(chatModel);
        return this;
    }

    public AgenticSupervisorBuilder chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        delegate.chatMemoryProvider(chatMemoryProvider);
        return this;
    }

    public AgenticSupervisorBuilder name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticSupervisorBuilder description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticSupervisorBuilder outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticSupervisorBuilder requestGenerator(Function<AgenticScope, String> requestGenerator) {
        delegate.requestGenerator(requestGenerator);
        return this;
    }

    public AgenticSupervisorBuilder contextGenerationStrategy(SupervisorContextStrategy contextGenerationStrategy) {
        delegate.contextGenerationStrategy(contextGenerationStrategy);
        return this;
    }

    public AgenticSupervisorBuilder responseStrategy(SupervisorResponseStrategy responseStrategy) {
        delegate.responseStrategy(responseStrategy);
        return this;
    }

    public AgenticSupervisorBuilder supervisorContext(String supervisorContext) {
        delegate.supervisorContext(supervisorContext);
        return this;
    }

    public AgenticSupervisorBuilder subAgents(Object... agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticSupervisorBuilder subAgents(Collection<?> agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticSupervisorBuilder maxAgentsInvocations(int maxAgentsInvocations) {
        delegate.maxAgentsInvocations(maxAgentsInvocations);
        return this;
    }

    public AgenticSupervisorBuilder output(Function<AgenticScope, Object> output) {
        delegate.output(output);
        return this;
    }

    public AgenticSupervisorBuilder errorHandler(
            Function<dev.langchain4j.agentic.agent.ErrorContext,
                    dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
        delegate.errorHandler(errorHandler);
        return this;
    }

    public AgenticSupervisorBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    public AgenticSupervisorBuilder beforeCall(Consumer<AgenticScope> beforeCall) {
        delegate.beforeCall(beforeCall);
        return this;
    }

    public AgenticSupervisorBuilder compensateOnError(boolean compensateOnError) {
        delegate.compensateOnError(compensateOnError);
        return this;
    }

    public AgenticSupervisor build() {
        return AgenticSupervisor.of(delegate.build());
    }
}
