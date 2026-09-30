package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.xinference;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.xinference.XinferenceChatModel;
import dev.langchain4j.community.model.xinference.XinferenceEmbeddingModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

/**
 * Xinference 工厂（自托管推理平台，OpenAI 兼容风格）：必填 {@code base-url} /
 * {@code model-name}，可选 {@code api-key}（本地部署可不填）。
 */
public class Langchain4jXinferenceModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "xinference";
    }

    @Override
    public XinferenceChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return XinferenceChatModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.string("api-key", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public XinferenceStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return XinferenceStreamingChatModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.string("api-key", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public XinferenceEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return XinferenceEmbeddingModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.string("api-key", null))
                .modelName(o.required("model-name"))
                .timeout(o.duration("timeout", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
