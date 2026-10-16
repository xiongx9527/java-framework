package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.function.Function;

/**
 * 单 agent（typed）构建器（隔离层）：传入带 {@code @Agent} 注解的接口，
 * 产物为该接口的可执行代理，可独立执行，也可作为 subAgent 挂入任意编排。
 *
 * <p>内部委托 {@link AgenticBuilder}，存储 / 上下文入口与其保持一致
 * （{@code memory} / {@code memoryStore} / {@code scopeStore} / {@code context} / {@code summarizedContext}）；
 * {@code ephemeral*} 与 {@code aiPersistence*} 等进阶记忆策略见 {@link AgenticBuilder}。</p>
 *
 * @param <T> 调用方声明的 agent 接口类型
 */
public final class AgenticTypedAgentBuilder<T> {

    private final AgenticBuilder<T> delegate;

    AgenticTypedAgentBuilder(AgentBuilder<T, ?> delegate) {
        this.delegate = new AgenticBuilder<>(delegate);
    }

    public AgenticTypedAgentBuilder<T> chatModel(ChatModel chatModel) {
        delegate.chatModel(chatModel);
        return this;
    }

    /** 流式模型：接口方法返回 {@code TokenStream} 时逐段推送（编排流式前提）。 */
    public AgenticTypedAgentBuilder<T> streamingChatModel(StreamingChatModel streamingChatModel) {
        delegate.streamingChatModel(streamingChatModel);
        return this;
    }

    public AgenticTypedAgentBuilder<T> chatMemory(ChatMemory chatMemory) {
        delegate.chatMemory(chatMemory);
        return this;
    }

    public AgenticTypedAgentBuilder<T> chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        delegate.chatMemoryProvider(chatMemoryProvider);
        return this;
    }

    public AgenticTypedAgentBuilder<T> systemMessage(String systemMessage) {
        delegate.systemMessage(systemMessage);
        return this;
    }

    public AgenticTypedAgentBuilder<T> userMessage(String userMessage) {
        delegate.userMessage(userMessage);
        return this;
    }

    public AgenticTypedAgentBuilder<T> tools(Object... tools) {
        delegate.tools(tools);
        return this;
    }

    public AgenticTypedAgentBuilder<T> toolProvider(ToolProvider toolProvider) {
        delegate.toolProvider(toolProvider);
        return this;
    }

    public AgenticTypedAgentBuilder<T> toolProviders(ToolProvider... toolProviders) {
        delegate.toolProvider(toolProviders);
        return this;
    }

    public AgenticTypedAgentBuilder<T> maxToolCallingRoundTrips(int maxToolCallingRoundTrips) {
        delegate.maxToolCallingRoundTrips(maxToolCallingRoundTrips);
        return this;
    }

    /** 高级 RAG 管线：自定义检索/排序/增强策略。 */
    public AgenticTypedAgentBuilder<T> retrievalAugmentor(RetrievalAugmentor retrievalAugmentor) {
        delegate.retrievalAugmentor(retrievalAugmentor);
        return this;
    }

    /** 简化 RAG：直接指定内容检索器（框架内部组装默认 RetrievalAugmentor）。 */
    public AgenticTypedAgentBuilder<T> contentRetriever(ContentRetriever contentRetriever) {
        delegate.contentRetriever(contentRetriever);
        return this;
    }

    public AgenticTypedAgentBuilder<T> name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticTypedAgentBuilder<T> description(String description) {
        delegate.description(description);
        return this;
    }

    public AgenticTypedAgentBuilder<T> outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticTypedAgentBuilder<T> async(boolean async) {
        delegate.async(async);
        return this;
    }

    public AgenticTypedAgentBuilder<T> listener(dev.langchain4j.agentic.observability.AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    /** 进程内记忆：每个 {@code memoryId} 一份窗口记忆（默认包 {@code EphemeralChatMemory}），窗口大小 {@code maxMessages}（默认 20）。 */
    public AgenticTypedAgentBuilder<T> memory(Integer maxMessages) {
        delegate.memory(maxMessages);
        return this;
    }

    /** 记忆存储：会话记忆（按 memoryId）落给定 {@link ChatMemoryStore}，可持久化。 */
    public AgenticTypedAgentBuilder<T> memoryStore(ChatMemoryStore memoryStore) {
        delegate.memoryStore(memoryStore);
        return this;
    }

    /** 按 token 截断记忆：配置后记忆改用 TokenWindowChatMemory（忽略 {@link #memory(Integer)}）。 */
    public AgenticTypedAgentBuilder<T> tokenizer(TokenCountEstimator tokenizer) {
        delegate.tokenizer(tokenizer);
        return this;
    }

    /** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 时生效，默认 1000）。 */
    public AgenticTypedAgentBuilder<T> maxTokens(Integer maxTokens) {
        delegate.maxTokens(maxTokens);
        return this;
    }

    /** agentic 原生存储：把编排共享状态（AgenticScope）持久化到给定 {@link AgenticScopeStore}（构建时安装，进程级生效）。 */
    public AgenticTypedAgentBuilder<T> scopeStore(AgenticScopeStore scopeStore) {
        delegate.scopeStore(scopeStore);
        return this;
    }

    /** 原生上下文注入：作为「agentic 系统」一员运行时，从共享 scope 读取上下文并包装进本 agent 的 prompt。 */
    public AgenticTypedAgentBuilder<T> context(Function<AgenticScope, String> contextProvider) {
        delegate.context(contextProvider);
        return this;
    }

    /** 摘要上下文：把指定名称 agents 的输出对话摘要后注入本 agent 的 prompt（仅系统内运行时生效）。 */
    public AgenticTypedAgentBuilder<T> summarizedContext(String... contextProvidingAgents) {
        delegate.summarizedContext(contextProvidingAgents);
        return this;
    }

    public T build() {
        return delegate.build();
    }
}
