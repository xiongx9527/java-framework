package com.xiongx9527.fw.plus.ai.rag;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.rag.query.Metadata;

/**
 * 检索增强请求：待增强的消息 + 检索上下文。
 *
 * <p><b>作用</b>：把「要增强哪条消息」与「检索时需要知道的上下文」一起传给
 * {@link RetrievalAugmentor}。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.AugmentationRequest}。</p>
 *
 * @author xiongxiao
 */
public class AugmentationRequest {

    private final ChatMessage chatMessage;
    private final Metadata metadata;

    /**
     * 构造请求。
     *
     * @param chatMessage 待增强的消息
     * @param metadata    检索上下文
     */
    public AugmentationRequest(ChatMessage chatMessage, Metadata metadata) {
        this.chatMessage = chatMessage;
        this.metadata = metadata;
    }

    /**
     * 待增强的消息。
     *
     * @return 消息
     */
    public ChatMessage chatMessage() {
        return chatMessage;
    }

    /**
     * 检索上下文。
     *
     * @return 元数据
     */
    public Metadata metadata() {
        return metadata;
    }
}
