package com.xiongx9527.fw.plus.ai.model.decision.request;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 对话消息型决策输入。
 *
 * <p><b>作用</b>：以一段对话历史作为决策依据，适用于「基于对话上下文做判定」的场景。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.MessagesInput}。</p>
 *
 * @author xiongxiao
 */
public final class MessagesInput implements DecisionInput {

    private final List<ChatMessage> messages;

    private MessagesInput(List<? extends ChatMessage> messages) {
        this.messages = Collections.unmodifiableList(new java.util.ArrayList<ChatMessage>(messages));
    }

    /**
     * 由对话消息创建。
     *
     * @param messages 消息列表
     * @return 输入对象
     */
    public static MessagesInput from(List<? extends ChatMessage> messages) {
        return new MessagesInput(messages);
    }

    /**
     * 对话消息列表。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<ChatMessage> messages() {
        return messages;
    }

    @Override
    public DecisionInputType type() {
        return DecisionInputType.MESSAGES;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessagesInput)) {
            return false;
        }
        return Objects.equals(messages, ((MessagesInput) o).messages);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messages);
    }

    @Override
    public String toString() {
        return "MessagesInput{messages=" + messages.size() + "}";
    }
}
