package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.chatglm;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.chatglm.ChatGlmChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

/**
 * 智谱 ChatGLM 工厂（community 版，与 zhipu-ai 二选一即可）：默认 base-url 为官方 API。
 * 注意：community 版只有同步 ChatModel，无 StreamingChatModel。
 */
public class Langchain4jChatGlmModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "chatglm";
    }

    @Override
    public ChatGlmChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return ChatGlmChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxLength(nullIfZero(o.integer("max-length", null)))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
