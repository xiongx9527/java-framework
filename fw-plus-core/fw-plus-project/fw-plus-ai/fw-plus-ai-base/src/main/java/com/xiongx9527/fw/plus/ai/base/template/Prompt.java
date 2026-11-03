package com.xiongx9527.fw.plus.ai.base.template;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 已填充好变量的提示词（消息列表）。
 */
public final class Prompt {

    private final List<ChatMessage> messages;

    private Prompt(List<ChatMessage> messages) {
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }

    public static Prompt of(List<ChatMessage> messages) {
        return new Prompt(messages == null ? new ArrayList<ChatMessage>() : messages);
    }

    public static Prompt of(ChatMessage message) {
        List<ChatMessage> list = new ArrayList<ChatMessage>();
        if (message != null) {
            list.add(message);
        }
        return new Prompt(list);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
