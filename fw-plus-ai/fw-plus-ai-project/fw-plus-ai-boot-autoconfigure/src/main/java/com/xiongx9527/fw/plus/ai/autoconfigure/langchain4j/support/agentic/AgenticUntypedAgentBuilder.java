package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.UntypedAgentBuilder;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.tool.ToolProvider;

/**
 * 单 agent（untyped）构建器（隔离层）：产物为 {@link UntypedAgent} 句柄，
 * 可独立执行，也可作为 subAgent 挂入任意编排。
 */
public final class AgenticUntypedAgentBuilder {

    private final UntypedAgentBuilder delegate;

    AgenticUntypedAgentBuilder(UntypedAgentBuilder delegate) {
        this.delegate = delegate;
    }

    public AgenticUntypedAgentBuilder chatModel(ChatModel chatModel) {
        delegate.chatModel(chatModel);
        return this;
    }

    public AgenticUntypedAgentBuilder chatMemory(ChatMemory chatMemory) {
        delegate.chatMemory(chatMemory);
        return this;
    }

    public AgenticUntypedAgentBuilder chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        delegate.chatMemoryProvider(chatMemoryProvider);
        return this;
    }

    public AgenticUntypedAgentBuilder systemMessage(String systemMessage) {
        delegate.systemMessage(systemMessage);
        return this;
    }

    public AgenticUntypedAgentBuilder userMessage(String userMessage) {
        delegate.userMessage(userMessage);
        return this;
    }

    public AgenticUntypedAgentBuilder tools(Object... tools) {
        delegate.tools(tools);
        return this;
    }

    public AgenticUntypedAgentBuilder toolProvider(ToolProvider toolProvider) {
        delegate.toolProvider(toolProvider);
        return this;
    }

    public AgenticUntypedAgentBuilder toolProviders(ToolProvider... toolProviders) {
        delegate.toolProviders(toolProviders);
        return this;
    }

    public AgenticUntypedAgentBuilder maxToolCallingRoundTrips(int maxToolCallingRoundTrips) {
        delegate.maxToolCallingRoundTrips(maxToolCallingRoundTrips);
        return this;
    }

    public AgenticUntypedAgentBuilder name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticUntypedAgentBuilder description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticUntypedAgentBuilder outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticUntypedAgentBuilder async(boolean async) {
        delegate.async(async);
        return this;
    }

    public AgenticUntypedAgentBuilder returnType(Class<?> returnType) {
        delegate.returnType(returnType);
        return this;
    }

    public AgenticUntypedAgentBuilder inputKey(Class<?> type, String name) {
        delegate.inputKey(type, name);
        return this;
    }

    public AgenticUntypedAgentBuilder inputKeys(Class<?> type1, String name1, Class<?> type2, String name2) {
        delegate.inputKeys(type1, name1, type2, name2);
        return this;
    }

    public AgenticUntypedAgentBuilder listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    public UntypedAgent build() {
        return delegate.build();
    }
}
