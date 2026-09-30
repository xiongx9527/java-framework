package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.cohere;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.CohereChatModel;
import dev.langchain4j.community.model.CohereStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

/**
 * Cohere 工厂（community 版）：必填 {@code api-key}，可选 {@code base-url}、
 * {@code thinking-type}（思考模式）等。
 */
public class Langchain4jCohereModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "cohere";
    }

    @Override
    public CohereChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return CohereChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stopSequences(o.stringList("stop", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public CohereStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return CohereStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", null))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stopSequences(o.stringList("stop", null))
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
