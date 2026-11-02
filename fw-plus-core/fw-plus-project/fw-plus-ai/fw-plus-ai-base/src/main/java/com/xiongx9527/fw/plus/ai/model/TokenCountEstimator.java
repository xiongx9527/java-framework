package com.xiongx9527.fw.plus.ai.model;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;

/** 分词计数端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.TokenCountEstimator}
 * （方法 estimateTokenCountInText / InMessage / InMessages；Spring AI 等价 TokenCountEstimator）。 */
public interface TokenCountEstimator {
    int estimateTokenCountInText(String text);
    int estimateTokenCountInMessage(ChatMessage message);
    int estimateTokenCountInMessages(Iterable<ChatMessage> messages);
}
