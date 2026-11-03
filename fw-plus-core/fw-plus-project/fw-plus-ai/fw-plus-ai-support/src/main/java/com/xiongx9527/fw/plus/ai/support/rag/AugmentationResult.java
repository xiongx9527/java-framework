package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 检索增强结果：命中的资料 + 已注入资料的消息。
 */
public final class AugmentationResult {

    private final List<Content> contents;

    private final List<ChatMessage> messages;

    private AugmentationResult(List<Content> contents, List<ChatMessage> messages) {
        this.contents = Collections.unmodifiableList(new ArrayList<Content>(contents));
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }

    public static AugmentationResult of(List<Content> contents, List<ChatMessage> messages) {
        return new AugmentationResult(contents == null ? new ArrayList<Content>() : contents,
                messages == null ? new ArrayList<ChatMessage>() : messages);
    }

    public List<Content> getContents() {
        return contents;
    }

    /** 可直接发给模型的消息。 */
    public List<ChatMessage> getMessages() {
        return messages;
    }
}
