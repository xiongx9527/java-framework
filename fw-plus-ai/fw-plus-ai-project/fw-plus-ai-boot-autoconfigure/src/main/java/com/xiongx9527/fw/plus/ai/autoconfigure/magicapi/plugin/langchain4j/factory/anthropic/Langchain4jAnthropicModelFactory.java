package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.anthropic;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import java.util.Collections;
import java.util.List;

/**
 * Anthropic 工厂：dev.langchain4j.model.anthropic.* builder 链。仅 chat/streaming-chat 两类。
 */
public class Langchain4jAnthropicModelFactory implements ModelFactory {

    private static final String DEFAULT_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    @Override
    public String provider() {
        return "anthropic";
    }

    @Override
    public AnthropicChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return AnthropicChatModel.builder()
                .apiKey(o.required("api-key"))
                .baseUrl(o.string("base-url", null))
                .version(o.string("version", DEFAULT_VERSION))
                .beta(o.string("beta", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", DEFAULT_MAX_TOKENS)))
                .stopSequences(o.stringList("stop-sequences", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public AnthropicStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        // 注意：0.35.0 streaming builder 无 maxRetries 与 beta
        return AnthropicStreamingChatModel.builder()
                .apiKey(o.required("api-key"))
                .baseUrl(o.string("base-url", null))
                .version(o.string("version", DEFAULT_VERSION))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", DEFAULT_MAX_TOKENS)))
                .stopSequences(o.stringList("stop-sequences", null))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
