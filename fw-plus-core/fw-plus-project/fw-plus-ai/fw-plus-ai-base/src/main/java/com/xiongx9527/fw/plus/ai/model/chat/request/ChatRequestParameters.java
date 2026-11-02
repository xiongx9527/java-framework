package com.xiongx9527.fw.plus.ai.model.chat.request;

import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.List;

/** 聊天请求参数，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.request.ChatRequestParameters}。 */
public interface ChatRequestParameters {
    String modelName();
    Double temperature();
    Double topP();
    Integer topK();
    Double frequencyPenalty();
    Double presencePenalty();
    Integer maxOutputTokens();
    List<String> stopSequences();
    List<ToolSpecification> toolSpecifications();
    ToolChoice toolChoice();
    ResponseFormat responseFormat();
    ChatRequestParameters overrideWith(ChatRequestParameters other);
    default ChatRequestParameters defaultedBy(ChatRequestParameters other) { return overrideWith(other); }
}
