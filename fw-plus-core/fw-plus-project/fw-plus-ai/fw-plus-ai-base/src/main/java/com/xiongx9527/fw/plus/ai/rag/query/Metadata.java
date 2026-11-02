package com.xiongx9527.fw.plus.ai.rag.query;

import com.xiongx9527.fw.plus.ai.invocation.InvocationContext;
import com.xiongx9527.fw.plus.ai.invocation.InvocationParameters;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.SystemMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 检索查询元数据：查询文本之外的全部上下文。
 *
 * <p><b>作用</b>：检索往往需要「这次查询是替谁发的」——当前用户消息、系统消息、对话记忆、
 * 记忆 ID、调用上下文与调用参数。查询改写（QueryTransformer）与路由（QueryRouter）依赖这些信息
 * 做指代消解与多路选择。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.query.Metadata}
 * （{@code chatMessage/systemMessage/chatMemoryId/chatMemory/invocationContext/invocationParameters/
 * from/builder}）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #chatMemory()} 只读且永不为 {@code null}。</p>
 *
 * @author xiongxiao
 */
public class Metadata {

    private final ChatMessage chatMessage;
    private final SystemMessage systemMessage;
    private final Object chatMemoryId;
    private final List<ChatMessage> chatMemory;
    private final InvocationContext invocationContext;
    private final InvocationParameters invocationParameters;

    /**
     * 由构建器创建。
     *
     * @param builder 构建器
     */
    public Metadata(Builder builder) {
        this.chatMessage = builder.chatMessage;
        this.systemMessage = builder.systemMessage;
        this.chatMemoryId = builder.chatMemoryId;
        this.chatMemory = builder.chatMemory == null
                ? Collections.<ChatMessage>emptyList()
                : Collections.unmodifiableList(new ArrayList<ChatMessage>(builder.chatMemory));
        this.invocationContext = builder.invocationContext;
        this.invocationParameters = builder.invocationParameters;
    }

    /**
     * 由当前消息、记忆 ID 与对话记忆创建。
     *
     * @param chatMessage  当前消息
     * @param chatMemoryId 记忆 ID
     * @param chatMemory   对话记忆
     */
    public Metadata(ChatMessage chatMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        this(chatMessage, null, chatMemoryId, chatMemory);
    }

    /**
     * 由当前消息、系统消息、记忆 ID 与对话记忆创建。
     *
     * @param chatMessage   当前消息
     * @param systemMessage 系统消息
     * @param chatMemoryId  记忆 ID
     * @param chatMemory    对话记忆
     */
    public Metadata(ChatMessage chatMessage, SystemMessage systemMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        this(builder().chatMessage(chatMessage).systemMessage(systemMessage)
                .chatMemoryId(chatMemoryId).chatMemory(chatMemory));
    }

    /**
     * 当前消息。
     *
     * @return 消息，可能为 {@code null}
     */
    public ChatMessage chatMessage() { return chatMessage; }

    /**
     * 系统消息。
     *
     * @return 系统消息，可能为 {@code null}
     */
    public SystemMessage systemMessage() { return systemMessage; }

    /**
     * 对话记忆 ID。
     *
     * @return 记忆 ID，可能为 {@code null}
     */
    public Object chatMemoryId() { return chatMemoryId; }

    /**
     * 对话记忆。
     *
     * @return 只读消息列表，永不为 {@code null}
     */
    public List<ChatMessage> chatMemory() { return chatMemory; }

    /**
     * 调用上下文。
     *
     * @return 调用上下文，可能为 {@code null}
     */
    public InvocationContext invocationContext() { return invocationContext; }

    /**
     * 调用参数。
     *
     * @return 调用参数，可能为 {@code null}
     */
    public InvocationParameters invocationParameters() { return invocationParameters; }

    @Override
    public String toString() {
        return "Metadata{chatMemoryId=" + chatMemoryId + ", chatMemory=" + chatMemory.size() + '}';
    }

    /**
     * 由当前消息、记忆 ID 与对话记忆创建。
     *
     * @param chatMessage  当前消息
     * @param chatMemoryId 记忆 ID
     * @param chatMemory   对话记忆
     * @return 元数据
     */
    public static Metadata from(ChatMessage chatMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        return new Metadata(chatMessage, chatMemoryId, chatMemory);
    }

    /**
     * 由当前消息、系统消息、记忆 ID 与对话记忆创建。
     *
     * @param chatMessage   当前消息
     * @param systemMessage 系统消息
     * @param chatMemoryId  记忆 ID
     * @param chatMemory    对话记忆
     * @return 元数据
     */
    public static Metadata from(ChatMessage chatMessage, SystemMessage systemMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        return new Metadata(chatMessage, systemMessage, chatMemoryId, chatMemory);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() { return new Builder(); }

    /** {@link Metadata} 构建器。 */
    public static final class Builder {
        private ChatMessage chatMessage;
        private SystemMessage systemMessage;
        private Object chatMemoryId;
        private List<ChatMessage> chatMemory;
        private InvocationContext invocationContext;
        private InvocationParameters invocationParameters;

        public Builder chatMessage(ChatMessage chatMessage) { this.chatMessage = chatMessage; return this; }
        public Builder systemMessage(SystemMessage systemMessage) { this.systemMessage = systemMessage; return this; }
        public Builder chatMemoryId(Object chatMemoryId) { this.chatMemoryId = chatMemoryId; return this; }
        public Builder chatMemory(List<ChatMessage> chatMemory) { this.chatMemory = chatMemory; return this; }
        public Builder invocationContext(InvocationContext invocationContext) {
            this.invocationContext = invocationContext;
            return this;
        }
        public Builder invocationParameters(InvocationParameters invocationParameters) {
            this.invocationParameters = invocationParameters;
            return this;
        }
        public Metadata build() { return new Metadata(this); }
    }
}
