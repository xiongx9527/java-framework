package com.xiongx9527.fw.plus.ai.model.chat;

import com.xiongx9527.fw.plus.ai.StreamingResponseHandler;
import com.xiongx9527.fw.plus.ai.message.AiMessage;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.UserMessage;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.Collections;
import java.util.List;

/** 流式聊天模型端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.StreamingChatModel}（1.x 已更名；Spring AI 等价 StreamingChatModel）。 */
public interface StreamingChatModel {
    default void chat(String userMessage, StreamingResponseHandler<AiMessage> handler) { chat(Collections.singletonList(UserMessage.from(userMessage)), handler); }
    void chat(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler);
    default void chat(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, StreamingResponseHandler<AiMessage> handler) { throw new UnsupportedOperationException(); }
}
