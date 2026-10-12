package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.mistralai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;

import java.util.List;

/**
 * Mistral AI 工厂（主线 stable）：默认 base-url 为官方 API。
 */
public class Langchain4jMistralAiModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "mistral-ai";
    }

    @Override
    public MistralAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return MistralAiChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .safePrompt(o.bool("safe-prompt", null))
                .randomSeed(nullIfZero(o.integer("random-seed", null)))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public MistralAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        MistralAiStreamingChatModel.MistralAiStreamingChatModelBuilder builder =
                MistralAiStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .safePrompt(o.bool("safe-prompt", null))
                .randomSeed(nullIfZero(o.integer("random-seed", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners);
        if (o.has("send-thinking")) {
            builder.sendThinking(o.bool("send-thinking", null));
        }
        if (o.has("return-thinking")) {
            builder.returnThinking(o.bool("return-thinking", null));
        }
        return builder.build();
    }

    @Override
    public MistralAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return MistralAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .modelName(o.required("model-name"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
