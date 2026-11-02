package com.xiongx9527.fw.plus.ai.rag;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.rag.content.Content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 检索增强结果：增强后的消息 + 本次用到的内容。
 *
 * <p><b>作用</b>：既返回注入后的消息，也回传实际使用的内容，便于审计「这段回答引用了哪些资料」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.AugmentationResult}。</p>
 *
 * @author xiongxiao
 */
public class AugmentationResult {

    private final ChatMessage chatMessage;
    private final List<Content> contents;

    /**
     * 构造结果。
     *
     * @param chatMessage 增强后的消息
     * @param contents    使用到的内容
     */
    public AugmentationResult(ChatMessage chatMessage, List<Content> contents) {
        this.chatMessage = chatMessage;
        this.contents = contents == null
                ? Collections.<Content>emptyList()
                : Collections.unmodifiableList(new ArrayList<Content>(contents));
    }

    /**
     * 增强后的消息。
     *
     * @return 消息
     */
    public ChatMessage chatMessage() {
        return chatMessage;
    }

    /**
     * 使用到的内容。
     *
     * @return 只读内容列表，永不为 {@code null}
     */
    public List<Content> contents() {
        return contents;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link AugmentationResult} 构建器。 */
    public static final class Builder {
        private ChatMessage chatMessage;
        private List<Content> contents;

        public Builder chatMessage(ChatMessage chatMessage) { this.chatMessage = chatMessage; return this; }
        public Builder contents(List<Content> contents) { this.contents = contents; return this; }
        public AugmentationResult build() { return new AugmentationResult(chatMessage, contents); }
    }
}
