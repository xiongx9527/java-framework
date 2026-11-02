package com.xiongx9527.fw.plus.ai.invocation;

import com.xiongx9527.fw.plus.ai.message.UserMessage;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequestParameters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 调用上下文：描述「谁、通过哪个接口、在什么时刻」发起了一次 AI 调用。
 *
 * <p><b>作用</b>：RAG 检索、工具执行、监听器都需要知道调用来源（接口名 / 方法名 / 参数 / 用户消息），
 * 才能做上下文注入、权限判定与审计。它随调用显式传递，而不是靠全局状态。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.invocation.InvocationContext}
 * （含 {@code builder()/toBuilder()} 与全部默认方法）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #methodArguments()} 只读；默认方法在参数缺失时返回 {@code null}
 * 或空集合，不抛异常。</p>
 *
 * @author xiongxiao
 */
public interface InvocationContext {

    /**
     * 调用唯一标识。
     *
     * @return UUID
     */
    UUID invocationId();

    /**
     * AI Service 接口全限定名。
     *
     * @return 接口名，可能为 {@code null}
     */
    String interfaceName();

    /**
     * 被调用的方法名。
     *
     * @return 方法名，可能为 {@code null}
     */
    String methodName();

    /**
     * 方法实参。
     *
     * @return 只读参数列表，永不为 {@code null}
     */
    List<Object> methodArguments();

    /**
     * 本次调用中的用户消息（取参数列表中最后一个 {@link UserMessage}）。
     *
     * @return 用户消息，可能为 {@code null}
     */
    default UserMessage userMessage() {
        List<Object> arguments = methodArguments();
        for (int i = arguments.size() - 1; i >= 0; i--) {
            Object argument = arguments.get(i);
            if (argument instanceof UserMessage) {
                return (UserMessage) argument;
            }
        }
        return null;
    }

    /**
     * 原始用户消息（未经模板变量替换等处理）。
     *
     * @return 用户消息，可能为 {@code null}
     */
    default UserMessage originalUserMessage() {
        return userMessage();
    }

    /**
     * 对话记忆 ID。
     *
     * @return 记忆 ID，可能为 {@code null}
     */
    Object chatMemoryId();

    /**
     * 默认请求参数（模型温度、最大 token 等）。
     *
     * @return 请求参数，可能为 {@code null}
     */
    default ChatRequestParameters defaultRequestParameters() {
        return null;
    }

    /**
     * 模型提供方。
     *
     * @return 提供方，默认 {@link ModelProvider#OTHER}
     */
    default ModelProvider modelProvider() {
        return ModelProvider.OTHER;
    }

    /**
     * 调用参数（业务透传键值）。
     *
     * @return 参数集，永不为 {@code null}
     */
    InvocationParameters invocationParameters();

    /**
     * 当前线程托管的资源。
     *
     * @return 资源表，永不为 {@code null}
     */
    default Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managedParameters() {
        return LangChain4jManaged.current();
    }

    /**
     * 调用发起时刻。
     *
     * @return 时间戳
     */
    Instant timestamp();

    /**
     * 以当前实例为模板派生构建器。
     *
     * @return 已填充当前字段的构建器
     */
    default Builder toBuilder() {
        return new Builder()
                .invocationId(invocationId())
                .interfaceName(interfaceName())
                .methodName(methodName())
                .methodArguments(methodArguments())
                .chatMemoryId(chatMemoryId())
                .defaultRequestParameters(defaultRequestParameters())
                .modelProvider(modelProvider())
                .invocationParameters(invocationParameters())
                .timestamp(timestamp());
    }

    /**
     * 创建构建器。
     *
     * @return 空构建器
     */
    static Builder builder() {
        return new Builder();
    }

    /** {@link InvocationContext} 构建器（同时实现接口，{@code build()} 返回自身）。 */
    final class Builder implements InvocationContext {
        private UUID invocationId = UUID.randomUUID();
        private String interfaceName;
        private String methodName;
        private List<Object> methodArguments = Collections.emptyList();
        private Object chatMemoryId;
        private ChatRequestParameters defaultRequestParameters;
        private ModelProvider modelProvider = ModelProvider.OTHER;
        private InvocationParameters invocationParameters = new InvocationParameters();
        private Instant timestamp = Instant.now();

        public Builder invocationId(UUID invocationId) { this.invocationId = invocationId; return this; }
        public Builder interfaceName(String interfaceName) { this.interfaceName = interfaceName; return this; }
        public Builder methodName(String methodName) { this.methodName = methodName; return this; }
        public Builder methodArguments(List<Object> methodArguments) {
            this.methodArguments = methodArguments == null
                    ? Collections.<Object>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Object>(methodArguments));
            return this;
        }
        public Builder chatMemoryId(Object chatMemoryId) { this.chatMemoryId = chatMemoryId; return this; }
        public Builder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }
        public Builder modelProvider(ModelProvider modelProvider) { this.modelProvider = modelProvider; return this; }
        public Builder invocationParameters(InvocationParameters invocationParameters) {
            this.invocationParameters = invocationParameters;
            return this;
        }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public InvocationContext build() { return this; }

        @Override public UUID invocationId() { return invocationId; }
        @Override public String interfaceName() { return interfaceName; }
        @Override public String methodName() { return methodName; }
        @Override public List<Object> methodArguments() { return methodArguments; }
        @Override public Object chatMemoryId() { return chatMemoryId; }
        @Override public ChatRequestParameters defaultRequestParameters() { return defaultRequestParameters; }
        @Override public ModelProvider modelProvider() { return modelProvider; }
        @Override public InvocationParameters invocationParameters() { return invocationParameters; }
        @Override public Instant timestamp() { return timestamp; }
    }
}
