package com.xiongx9527.fw.plus.ai.model.chat;

import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.message.AiMessage;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.ToolExecutionResultMessage;
import com.xiongx9527.fw.plus.ai.message.UserMessage;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequest;
import com.xiongx9527.fw.plus.ai.model.chat.response.ChatResponse;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 同步聊天模型端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.ChatModel}（1.x 已更名；Spring AI 等价 ChatModel）。 */
public interface ChatModel {

    /** 完整请求模型入口（langchain4j 1.23.0 的 {@code chat(ChatRequest)}）。 */
    default ChatResponse chat(ChatRequest request) { throw new UnsupportedOperationException(); }

    default Response<AiMessage> chat(String userMessage) { return chat(Collections.singletonList(UserMessage.from(userMessage))); }
    default Response<AiMessage> chat(ChatMessage... messages) { return chat(Arrays.asList(messages)); }
    default Response<AiMessage> chat(List<ChatMessage> messages) { throw new UnsupportedOperationException(); }
    default Response<AiMessage> chat(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) { throw new UnsupportedOperationException(); }
    default Response<AiMessage> chat(List<ChatMessage> messages, ToolSpecification toolSpecification) { return chat(messages, Collections.singletonList(toolSpecification)); }
    default Response<AiMessage> chat(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications, ToolExecutionResultMessage... toolExecutionResultMessages) { throw new UnsupportedOperationException(); }
}
