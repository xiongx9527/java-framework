package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.ParallelMapperService;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 并行映射编排构建器（隔离层）：对 itemsProvider 指定的列表逐项并行执行映射 agent，
 * 结果列表汇聚写回 outputKey。
 *
 * <p>映射 agent 需带有 outputKey（例如带 {@code @Agent(outputKey = "...")} 的接口/POJO），
 * 底层以「outputKey_索引」逐项写回后再汇聚为列表。</p>
 */
public final class AgenticParallelMapperBuilder {

    private final ParallelMapperService<UntypedAgent> delegate;

    AgenticParallelMapperBuilder(ParallelMapperService<UntypedAgent> delegate) {
        this.delegate = delegate;
    }

    public AgenticParallelMapperBuilder itemsProvider(String itemsProvider) {
        delegate.itemsProvider(itemsProvider);
        return this;
    }

    public AgenticParallelMapperBuilder subAgents(Object... agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticParallelMapperBuilder subAgents(Collection<?> agents) {
        delegate.subAgents(AgenticAgents.unwrapAll(agents));
        return this;
    }

    public AgenticParallelMapperBuilder executor(Executor executor) {
        delegate.executor(executor);
        return this;
    }

    public AgenticParallelMapperBuilder name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticParallelMapperBuilder description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticParallelMapperBuilder outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticParallelMapperBuilder output(Function<dev.langchain4j.agentic.scope.AgenticScope, Object> output) {
        delegate.output(output);
        return this;
    }

    public AgenticParallelMapperBuilder beforeCall(Consumer<dev.langchain4j.agentic.scope.AgenticScope> beforeCall) {
        delegate.beforeCall(beforeCall);
        return this;
    }

    public AgenticParallelMapperBuilder compensateOnError(boolean compensateOnError) {
        delegate.compensateOnError(compensateOnError);
        return this;
    }

    public AgenticParallelMapperBuilder errorHandler(
            Function<dev.langchain4j.agentic.agent.ErrorContext,
                    dev.langchain4j.agentic.agent.ErrorRecoveryResult> errorHandler) {
        delegate.errorHandler(errorHandler);
        return this;
    }

    public AgenticParallelMapperBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    public UntypedAgent build() {
        return delegate.build();
    }
}
