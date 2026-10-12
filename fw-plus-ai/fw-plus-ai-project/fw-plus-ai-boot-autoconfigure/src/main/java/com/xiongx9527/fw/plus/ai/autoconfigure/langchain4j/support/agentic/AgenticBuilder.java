package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.agent.UntypedAgentBuilder;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.function.Function;

/**
 * 单 agent 构建器（隔离层）：对标 {@code AssistantBuilder} 的「静态工厂 → 流式累加 → build() 产出」
 * 流程，用于构建<b>一个</b> agent——封装 {@link AgenticServices#agentBuilder()}
 * 与 {@link AgenticServices#agentBuilder(Class)}。
 *
 * <p>一个泛型类同时覆盖 typed 与 untyped 两条路径
 * （上游 {@code UntypedAgentBuilder extends AgentBuilder<UntypedAgent, UntypedAgentBuilder>}）：</p>
 * <pre>{@code
 * // typed：传入带 @Agent 注解的接口，产物为该接口代理
 * EchoAgent echo = AgenticBuilder.agent(EchoAgent.class)
 *         .chatModel(model).systemMessage("你是回显助手").tools(svc).outputKey("out").build();
 *
 * // untyped：产物为 UntypedAgent 句柄
 * UntypedAgent agent = AgenticBuilder.agent()
 *         .chatModel(model).returnType(String.class).inputKey(String.class, "question")
 *         .userMessage("回答：{{question}}").outputKey("answer").build();
 * }</pre>
 *
 * <p>隔离层说明：所有对 langchain4j-agentic 实验性 API 的引用都收口在本类（及同级门面）之内，
 * 业务/脚本用本类即可构建 agent，无需直接接触上游 {@code AgentBuilder}。</p>
 *
 * @param <T> typed 时为调用方声明的 {@code @Agent} 接口类型；untyped 时为 {@link UntypedAgent}
 */
public final class AgenticBuilder<T> {

    private final AgentBuilder<T, ?> delegate;

    private AgenticBuilder(AgentBuilder<T, ?> delegate) {
        this.delegate = delegate;
    }

    /** 单 agent（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的可执行代理。 */
    public static <T> AgenticBuilder<T> agent(Class<T> agentServiceClass) {
        return new AgenticBuilder<>(AgenticServices.agentBuilder(agentServiceClass));
    }

    /** 单 agent（untyped）：产物为 {@link UntypedAgent} 句柄。 */
    public static AgenticBuilder<UntypedAgent> agent() {
        return new AgenticBuilder<>(AgenticServices.agentBuilder());
    }

    /** 对话模型（必填）：agent 调用 LLM 时使用。 */
    public AgenticBuilder<T> chatModel(ChatModel model) {
        delegate.chatModel(model);
        return this;
    }

    /** 流式模型：接口方法返回 {@code TokenStream} 时逐段推送。 */
    public AgenticBuilder<T> streamingChatModel(StreamingChatModel model) {
        delegate.streamingChatModel(model);
        return this;
    }

    /** 会话记忆（单份）：默认按 memoryId 切分当 untyped 时以 no-arg 形式使用。 */
    public AgenticBuilder<T> chatMemory(ChatMemory chatMemory) {
        delegate.chatMemory(chatMemory);
        return this;
    }

    /** 会话记忆提供者：按 memoryId 提供记忆。 */
    public AgenticBuilder<T> chatMemoryProvider(ChatMemoryProvider provider) {
        delegate.chatMemoryProvider(provider);
        return this;
    }

    /** 系统提示：固定文本。 */
    public AgenticBuilder<T> systemMessage(String text) {
        delegate.systemMessage(text);
        return this;
    }

    /** 系统提示：自定义 provider（入参为 AgenticScope 的相关键）。 */
    public AgenticBuilder<T> systemMessageProvider(Function<Object, String> provider) {
        delegate.systemMessageProvider(provider);
        return this;
    }

    /** 用户消息模板（untyped 常用，如 {@code "回答：{{question}}"}）。 */
    public AgenticBuilder<T> userMessage(String userMessage) {
        delegate.userMessage(userMessage);
        return this;
    }

    /** 工具对象（含 {@code @Tool} 方法）：可多次调用累积。 */
    public AgenticBuilder<T> tools(Object... toolObjects) {
        delegate.tools(toolObjects);
        return this;
    }

    /** 工具提供者：可多次调用累积（等价 {@code toolProvider(...)} 的变长形态）。 */
    public AgenticBuilder<T> toolProvider(ToolProvider... providers) {
        delegate.toolProviders(providers);
        return this;
    }

    /** 简化 RAG：直接指定内容检索器。 */
    public AgenticBuilder<T> contentRetriever(ContentRetriever contentRetriever) {
        delegate.contentRetriever(contentRetriever);
        return this;
    }

    /** 高级 RAG 管线：自定义检索/排序/增强策略。 */
    public AgenticBuilder<T> retrievalAugmentor(RetrievalAugmentor retrievalAugmentor) {
        delegate.retrievalAugmentor(retrievalAugmentor);
        return this;
    }

    /** 工具循环最大轮数。 */
    public AgenticBuilder<T> maxToolCallingRoundTrips(int maxToolCallingRoundTrips) {
        delegate.maxToolCallingRoundTrips(maxToolCallingRoundTrips);
        return this;
    }

    public AgenticBuilder<T> name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticBuilder<T> description(String description) {
        delegate.description(description);
        return this;
    }

    /** 输出键：agent 输出写入 AgenticScope 的键名。 */
    public AgenticBuilder<T> outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticBuilder<T> async(boolean async) {
        delegate.async(async);
        return this;
    }

    /** 观测监听器：观察 agent 调用前后、工具执行、错误等。 */
    public AgenticBuilder<T> listener(AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    // ------------------------------------------------------------------
    // untyped 专属（typed 构建器调用抛 IllegalStateException）
    // ------------------------------------------------------------------

    /** untyped 专属：显式声明产物返回类型（typed 由接口方法签名决定，无需调用）。 */
    public AgenticBuilder<T> returnType(Class<?> returnType) {
        untyped().returnType(returnType);
        return this;
    }

    /** untyped 专属：声明一个输入键（作为 AgenticScope 输入变量）。 */
    public AgenticBuilder<T> inputKey(Class<?> type, String name) {
        untyped().inputKey(type, name);
        return this;
    }

    /** untyped 专属：声明两个输入键。 */
    public AgenticBuilder<T> inputKeys(Class<?> type1, String name1, Class<?> type2, String name2) {
        untyped().inputKeys(type1, name1, type2, name2);
        return this;
    }

    private UntypedAgentBuilder untyped() {
        if (delegate instanceof UntypedAgentBuilder untyped) {
            return untyped;
        }
        throw new IllegalStateException("returnType/inputKey/inputKeys 仅适用于 untyped agent（用 AgenticBuilder.agent() 构建）");
    }

    /** 产出 agent：typed 返回接口代理，untyped 返回 {@link UntypedAgent}。 */
    public T build() {
        return delegate.build();
    }
}
