package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 人工介入节点构建器（隔离层）：编排执行到该节点时采用外部提供的
 * responseProvider 响应继续流转，产物可作为任意编排的 subAgent 挂入。
 */
public final class AgenticHumanInTheLoopBuilder {

    private final HumanInTheLoop.HumanInTheLoopBuilder delegate = AgenticServices.humanInTheLoopBuilder();

    AgenticHumanInTheLoopBuilder() {
    }

    /** 固定响应提供者。 */
    public AgenticHumanInTheLoopBuilder responseProvider(Supplier<?> responseProvider) {
        delegate.responseProvider(responseProvider);
        return this;
    }

    /** 基于 AgenticScope 动态计算响应。 */
    public AgenticHumanInTheLoopBuilder responseProvider(Function<AgenticScope, ?> responseProvider) {
        delegate.responseProvider(responseProvider);
        return this;
    }

    public AgenticHumanInTheLoopBuilder outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticHumanInTheLoopBuilder description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticHumanInTheLoopBuilder async(boolean async) {
        delegate.async(async);
        return this;
    }

    public AgenticHumanInTheLoopBuilder inputKey(Class<?> type, String name) {
        delegate.inputKey(type, name);
        return this;
    }

    public AgenticHumanInTheLoopBuilder inputKeys(Class<?> type1, String name1, Class<?> type2, String name2) {
        delegate.inputKeys(type1, name1, type2, name2);
        return this;
    }

    public AgenticHumanInTheLoopBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    public AgenticHumanInTheLoop build() {
        return AgenticHumanInTheLoop.of(delegate.build());
    }
}
